package uk.ac.manchester.cs.spinnaker.model;

public class CollabContext {
    private String appId;
    private Collab collab;
    private String context;
    private int id;
    private String name;
    private int orderIndex;
    private int parent;
    private String type;

    public String getAppId() {
        return appId;
    }

    public void setAppId(final String appId) {
        this.appId = appId;
    }

    public Collab getCollab() {
        return collab;
    }

    public void setCollab(final Collab collab) {
        this.collab = collab;
    }

    public String getContext() {
        return context;
    }

    public void setContext(final String context) {
        this.context = context;
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(final int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public int getParent() {
        return parent;
    }

    public void setParent(final int parent) {
        this.parent = parent;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }
}
