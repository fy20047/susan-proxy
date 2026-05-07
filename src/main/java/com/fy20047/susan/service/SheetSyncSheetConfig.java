package com.fy20047.susan.service;

public class SheetSyncSheetConfig {

    private final boolean visible;
    private final Boolean preorder;

    public SheetSyncSheetConfig(boolean visible, Boolean preorder) {
        this.visible = visible;
        this.preorder = preorder;
    }

    public boolean isVisible() {
        return visible;
    }

    public Boolean getPreorder() {
        return preorder;
    }
}
