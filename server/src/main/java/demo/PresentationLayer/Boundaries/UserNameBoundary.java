package demo.PresentationLayer.Boundaries;

public class UserNameBoundary {
    private String first;
    private String last;

    public UserNameBoundary() {
    }

    public UserNameBoundary(String first, String last) {
        this.first = first;
        this.last = last;
    }

    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    public String getLast() {
        return last;
    }

    public void setLast(String last) {
        this.last = last;
    }

    @Override
    public String toString() {
        return "first=" + first + ", last=" + last;
    }

}
