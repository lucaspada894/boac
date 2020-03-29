package boa.functions.paper;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import boa.functions.FunctionSpec;
import boa.types.Toplevel.Author;
import boa.types.Toplevel.Paper;
import boa.types.Toplevel.Paragraph;
import boa.types.Toplevel.Reference;
import boa.types.Toplevel.Section;

/**
 * @author yijiahuang
 *
 */
public class BoaPaperIntrinsics {

	@FunctionSpec(name = "search_title", returnType = "bool", formalParameters = { "string", "Paper" })
	public static boolean searchTitle(final String keyword, final Paper p) {
		if (p.getMetadata().hasTitle() && searchKeyword(p.getMetadata().getTitle(), keyword))
			return true;
		return false;
	}

	@FunctionSpec(name = "search_abstract", returnType = "bool", formalParameters = { "string", "Paper" })
	public static boolean searchAbstract(final String keyword, final Paper p) {
		for (Paragraph para : p.getAbstractList())
			if (searchKeyword(para.getText(), keyword))
				return true;
		return false;
	}

	@FunctionSpec(name = "search_body", returnType = "bool", formalParameters = { "string", "Paper" })
	public static boolean searchBody(final String keyword, final Paper p) {
		for (Section sec : p.getBodyTextList()) {
			if (searchKeyword(sec.getTitle(), keyword))
				return true;
			for (Paragraph para : sec.getBodyList())
				if (searchKeyword(para.getText(), keyword))
					return true;
		}
		return false;
	}

	@FunctionSpec(name = "search_paper", returnType = "bool", formalParameters = { "string", "Paper" })
	public static boolean searchPaper(final String keyword, final Paper p) {
		if (searchTitle(keyword, p))
			return true;
		if (searchAbstract(keyword, p))
			return true;
		if (searchBody(keyword, p))
			return true;
		return false;
	}

	/**
	 * @param from    A time range start with MM/dd/yyyy inclusive
	 * @param to      A time range end with MM/dd/yyyy inclusive
	 * @param p       A Paper instance
	 * @return boolean
	 */
	@FunctionSpec(name = "search_paper", returnType = "bool", formalParameters = { "string", "string", "Paper" })
	public static boolean searchPaper(final String from, final String to, final Paper p) {
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		long left = -1, right = -1;
		try {
			left = df.parse(from).getTime() * 1000;
			right = df.parse(to).getTime() * 1000;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		long publishTime = p.getMetadata().getPublishTime();
		if (publishTime >= left && publishTime <= right)
			return true;
		return false;
	}
	
	@FunctionSpec(name = "search_citation", returnType = "bool", formalParameters = { "string", "Paper" })
	public static boolean searchCitation(final String title, final Paper p) {
		for (Reference ref : p.getBibEntriesList())
			if (ref.getTitle().equals(title))
				return true;
		return false;
	}
	
	@FunctionSpec(name = "search_keyword", returnType = "bool", formalParameters = { "string", "string" })
	public static boolean searchKeyword(String src, String what) {
	    final int length = what.length();
	    if (length == 0)
	        return true; // Empty string is contained

	    final char firstLo = Character.toLowerCase(what.charAt(0));
	    final char firstUp = Character.toUpperCase(what.charAt(0));

	    for (int i = src.length() - length; i >= 0; i--) {
	        // Quick check before calling the more expensive regionMatches() method:
	        final char ch = src.charAt(i);
	        if (ch != firstLo && ch != firstUp)
	            continue;

	        if (src.regionMatches(true, i, what, 0, length))
	            return true;
	    }

	    return false;
	}
	
	@FunctionSpec(name = "pretty_print", returnType = "string", formalParameters = { "Paper" })
	public static String prettyPrint(Paper p) {
		String s = "";
		int lastIdx = p.getMetadata().getAuthorsCount() - 1;
		for (int i = 0; i < p.getMetadata().getAuthorsCount(); i++) {
			if (i == 0) {
				s += getAuthor(p.getMetadata().getAuthors(i));
			} else if (i == lastIdx) {
				if (lastIdx > 1)
					s += ",";
				s += " and " + getAuthor(p.getMetadata().getAuthors(i));
			} else {
				s += ", " + getAuthor(p.getMetadata().getAuthors(i));
			}
		}
		s += ".";
		return s;
	}
	
	private static String getAuthor(Author author) {
		String s = "";
		s += author.getFirst() + " ";
		String mid = "";
		for (String middle : author.getMiddleList())
			for (String comp : middle.split(" "))
				mid += comp + ".";
		if (!mid.equals(""))
			s += mid + " ";
		 s += author.getLast();
		return s;
	}

}
