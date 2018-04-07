package boa.graphs.cdg;


public class CDGEdge {

    private CDGNode src;
    private CDGNode dest;
    private String label;

    public CDGEdge(CDGNode src, CDGNode dest) {
        this.src = src;
        this.dest = dest;
    }

    public CDGEdge(CDGNode src, CDGNode dest, String label) {
        this.src = src;
        this.dest = dest;
        this.label = label;
    }

    //Setters
    public void setSrc(CDGNode src) {
        this.src = src;
    }

    public void setDest(CDGNode dest) {
        this.dest = dest;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    //Getters
    public CDGNode getSrc() {
        return src;
    }

    public CDGNode getDest() {
        return dest;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CDGEdge cdgEdge = (CDGEdge) o;

        if (!src.equals(cdgEdge.src)) return false;
        if (!dest.equals(cdgEdge.dest)) return false;
        return label.equals(cdgEdge.label);
    }

    @Override
    public int hashCode() {
        int result = src.hashCode();
        result = 31 * result + dest.hashCode();
        result = 31 * result + label.hashCode();
        return result;
    }
}
