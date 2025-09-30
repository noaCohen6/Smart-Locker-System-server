package demo.PresentationLayer.Boundaries;

import demo.DataAccessLayer.IDs.ObjectID;

public class ChildIdWrapper {
    private ObjectID childId;

    public ChildIdWrapper() {
    }

    public ChildIdWrapper(ObjectID childId) {
        this.childId = childId;
    }

    public ObjectID getChildId() {
        return childId;
    }

    public void setChildId(ObjectID childId) {
        this.childId = childId;
    }

    @Override
    public String toString() {
        return "ChildIdWrapper{" +
                "childId=" + childId +
                '}';
    }
}