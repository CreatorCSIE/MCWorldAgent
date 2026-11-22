public class fh {
    public fi[] a = new fi[37];
    public fi[] b = new fi[4];
    public fi[] c = new fi[4];
    public int d;
    public fi e;
    public boolean f;
    private dv g;
    public fh(dv p) { g = p; }
    public boolean a(fi s) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] == null) { a[i] = s; return true; }
        }
        return false;
    }
    public fi a() { return null; }
}
