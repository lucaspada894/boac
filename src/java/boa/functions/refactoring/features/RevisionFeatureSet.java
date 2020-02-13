package boa.functions.refactoring.features;

import static boa.functions.BoaMetricIntrinsics.getMetrics;
import static boa.functions.refactoring.BoaRefactoringIntrinsics.getCKStats;
import static boa.functions.refactoring.BoaRefactoringIntrinsics.getPackageNameFromFQN;
import static boa.functions.refactoring.BoaRefactoringIntrinsics.getStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import boa.functions.refactoring.BoaRefactoringPredictionIntrinsics;
import boa.functions.refactoring.Rev;
import boa.runtime.BoaAbstractVisitor;
import boa.types.Ast.Declaration;
import boa.types.Ast.Namespace;
import boa.types.Diff.ChangedFile;

public class RevisionFeatureSet {

	// ---- FEATURES start ------
	int nContributorInSnapshot = 0; // num of contributors
	int nPackageInSnapshot = 0; // num of packages
	int nFileInSnapshot = 0;
	int nClassInSnapshot = 0; // num of classes
	int nMethodInSnapshot = 0; // num of methods
	int nFieldInSnapshot = 0; // num of fields
	int nASTNodeInSnapshot = 0; // num of AST nodes

	// ---- statistics(min, max, mean, median, std) of the entire snapshot
	double[][] ckStats = null; // C&K metrics - rows: wmc, rfc, lcom, dit, noc, cbo
	double[] classStatsInPacakge = null; // at package-level
	double[] methodStatsInClass = null; // at class-level
	double[] fieldStatsInClass = null; // at class-level
	double[] astStatsInClass = null; // at class-level
	double[] astStatsInMethod = null; // at method-level
	// ---- FEATURES end ------

	private HashMap<String, double[]> metrics = null;
	HashMap<String, ClassFeatureSet> classFeatures = new HashMap<String, ClassFeatureSet>();
	private HashMap<String, Integer> pkgToClassNum = new HashMap<String, Integer>();
	private List<Integer> methodNums = new ArrayList<Integer>();
	private List<Integer> fieldNums = new ArrayList<Integer>();
	private List<Integer> astNodeNumsInClass = new ArrayList<Integer>();
	private List<Integer> astNodeNumsInMethod = new ArrayList<Integer>();

	private BoaAbstractVisitor fileVisitor = new BoaAbstractVisitor() {
		private List<Declaration> decls = new ArrayList<Declaration>();
		private String fileName = null;
		private int revIdx = -1;
		private int fileIdx = -1;

		@Override
		public boolean preVisit(final ChangedFile cf) throws Exception {
			this.fileName = cf.getName();
			this.revIdx = cf.getRevisionIdx();
			this.fileIdx = cf.getFileIdx();
			return true;
		}

		@Override
		public boolean preVisit(final Declaration node) throws Exception {
			decls.add(node);
			for (Declaration d : node.getNestedDeclarationsList())
				visit(d);
			return false;
		}

		@Override
		public boolean preVisit(final Namespace node) throws Exception {
			if (!pkgToClassNum.containsKey(node.getName()))
				pkgToClassNum.put(node.getName(), 0);
			pkgToClassNum.put(node.getName(), pkgToClassNum.get(node.getName()) + 1);

			boolean isTestFile = false;
			for (String importLib : node.getImportsList())
				if (importLib.contains("junit")) {
					isTestFile = true;
					break;
				}

			for (Declaration d : node.getDeclarationsList())
				visit(d); // collect all decls
			for (Declaration d : decls) {
				String classKey = fileName + " " + d.getFullyQualifiedName();
				ClassFeatureSet cfs = new ClassFeatureSet(d, metrics.get(classKey));
				if (isTestFile)
					cfs.isTestClass = 1;
				classFeatures.put(revIdx + " " + fileIdx, cfs); // visit each decl
				nMethodInSnapshot += cfs.getMethodFeatureSets().size();
				methodNums.add(cfs.getMethodFeatureSets().size());
				for (MethodFeatureSet mfs : cfs.getMethodFeatureSets())
					astNodeNumsInMethod.add(mfs.nASTNodeInMethod);
				nFieldInSnapshot += cfs.nFieldInClass;
				fieldNums.add(cfs.nFieldInClass);
				nASTNodeInSnapshot += cfs.nASTNodeInClass;
				astNodeNumsInClass.add(cfs.nASTNodeInClass);
			}
			nClassInSnapshot += decls.size();
			decls.clear();
			fileName = null;
			return false;
		}
	};

	public RevisionFeatureSet(ChangedFile[] snapshot, Rev rev) throws Exception {
		this.metrics = getMetrics(snapshot);
		this.ckStats = getCKStats(metrics);

		int count = 0;
		for (ChangedFile cf : snapshot) {
			nFileInSnapshot++;
			fileVisitor.visit(cf);

//			if (count++ == 0)
//				break;
		}

		this.nContributorInSnapshot = rev.nContributorSoFar;
		this.nPackageInSnapshot = pkgToClassNum.size();
		this.classStatsInPacakge = getStats(pkgToClassNum.values());
		this.methodStatsInClass = getStats(methodNums);
		this.fieldStatsInClass = getStats(fieldNums);
		this.astStatsInClass = getStats(astNodeNumsInClass);
		this.astStatsInMethod = getStats(astNodeNumsInMethod);
	}

	// output string column names
	private List<String> cols = new ArrayList<String>();
	private String[] ck = new String[] { "wmc", "rfc", "lcom", "dit", "noc", "cbo" };
	private String[] stat = new String[] { "min", "max", "mean", "median", "std" };
	private boolean updateCols = true;

	public List<String> toOutputLists(HashSet<String> refNodeLocs, HashSet<String> noRefNodeLocs) {
		List<String> outputs = featuresToString(noRefNodeLocs, 0);
		outputs.addAll(featuresToString(refNodeLocs, 1));
//		System.out.println(cols.size() + " " + outputs.get(0).split(" ").length + " " + outputs.size());
		return outputs;
	}

	private List<String> featuresToString(HashSet<String> set, int label) {
		List<String> outputs = new ArrayList<String>();
		JsonObject obj = BoaRefactoringPredictionIntrinsics.gson.toJsonTree(this).getAsJsonObject();
		StringBuilder revSB = new StringBuilder();

		for (Entry<String, JsonElement> entry : obj.entrySet()) {
			String key = entry.getKey();
			JsonElement je = entry.getValue();
			if (je.isJsonPrimitive()) {
//				System.out.println(key + " " + je.getAsString());
				if (updateCols)
					cols.add(key);
				revSB.append(je.getAsString() + " ");
			} else if (je.isJsonArray()) {
				if (key.equals("ckStats")) {
					JsonArray cks = je.getAsJsonArray();
					for (int i = 0; i < cks.size(); i++)
						updateStatsOutputs(revSB, ck[i], cks.get(i));
				} else {
					updateStatsOutputs(revSB, key, je);
				}
			} else if (je.isJsonObject()) {
				Iterator<Entry<String, JsonElement>> itr = je.getAsJsonObject().entrySet().iterator();
				while (itr.hasNext()) {
					Entry<String, JsonElement> e = itr.next();
					if (set.contains(e.getKey()))
						outputs.add(getClassOutput(e.getValue(), new StringBuilder(revSB), label));
				}
			}
		}
		return outputs;
	}

	private void updateStatsOutputs(StringBuilder revSB, String key, JsonElement je) {
		JsonArray ja = je.getAsJsonArray();
		for (int j = 0; j < ja.size(); j++) {
			String k = key + "_" + stat[j];
			String v = ja.get(j).getAsString();
			if (updateCols)
				cols.add(k);
			revSB.append(v + " ");
//			System.out.println(k + " " + v);
		}
	}

	private String getClassOutput(JsonElement je, StringBuilder sb, int label) {
		for (Entry<String, JsonElement> entry : je.getAsJsonObject().entrySet()) {
			String key = entry.getKey();
			JsonElement e = entry.getValue();
			if (e.isJsonPrimitive()) {
				if (updateCols)
					cols.add(key);
				sb.append(e.getAsString() + " ");
//				System.out.println(key + " " + e.getAsString());
			} else if (e.isJsonArray()) {
				if (key.equals("methodFeatureSets")) {
					if (updateCols)
						cols.add(key);
					sb.append(e.getAsJsonArray().size() + " ");
//					System.out.println(key + " " + e.getAsJsonArray().size());
				} else if (key.equals("ckInClass")) {
					JsonArray cks = e.getAsJsonArray();
					for (int i = 0; i < cks.size(); i++) {
						if (updateCols)
							cols.add(ck[i]);
						sb.append(cks.get(i).getAsString() + " ");
//						System.out.println(ck[i] + " " + cks.get(i).getAsString());
					}
				} else {
					updateStatsOutputs(sb, key, e);
				}
			}
		}
		if (updateCols)
			cols.add("label");
		sb.append(label + " ");
//		System.out.println("label " + label);
		updateCols = false;
		return sb.toString();
	}

	@Override
	public String toString() {
		return BoaRefactoringPredictionIntrinsics.gson.toJson(this);
	}
}