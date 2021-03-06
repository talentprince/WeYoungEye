// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.content.browser;

import org.chromium.chrome.testshell.R;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.provider.Browser;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;


/**
 * An ActionMode.Callback for in-page selection. This class handles both the editable and
 * non-editable cases.
 */
public class SelectActionModeCallback implements ActionMode.Callback {
    private static final int SELECT_ALL_ATTR_INDEX = 0;
    private static final int CUT_ATTR_INDEX = 1;
    private static final int COPY_ATTR_INDEX = 2;
    private static final int PASTE_ATTR_INDEX = 3;
    private static final int SHARE_ATTR_INDEX = 4;
    private static final int WEB_SEARCH_ATTR_INDEX = 5;
    private static final int[] ACTION_MODE_ATTRS = {
        android.R.attr.actionModeSelectAllDrawable,
        android.R.attr.actionModeCutDrawable,
        android.R.attr.actionModeCopyDrawable,
        android.R.attr.actionModePasteDrawable,
        R.attr.action_mode_share_drawable,
        R.attr.action_mode_web_search_drawable
    };

    private static final int ID_SELECTALL = 0;
    private static final int ID_COPY = 1;
    private static final int ID_SHARE = 2;
    private static final int ID_SEARCH = 3;
    private static final int ID_CUT = 4;
    private static final int ID_PASTE = 5;

    /**
     * An interface to retrieve information about the current selection, and also to perform
     * actions based on the selection or when the action bar is dismissed.
     */
    public interface ActionHandler {
        /**
         * Perform a select all action.
         */
        void selectAll();

        /**
         * Perform a copy (to clipboard) action.
         */
        void copy();

        /**
         * Perform a cut (to clipboard) action.
         */
        void cut();

        /**
         * Perform a paste action.
         */
        void paste();

        /**
         * Perform a share action.
         */
        void share();

        /**
         * Perform a search action.
         */
        void search();

        /**
         * @return true iff the current selection is editable (e.g. text within an input field).
         */
        boolean isSelectionEditable();

        /**
         * Called when the onDestroyActionMode of the SelectActionmodeCallback is called.
         */
        void onDestroyActionMode();

        /**
         * @return Whether or not share is available.
         */
        boolean isShareAvailable();

        /**
         * @return Whether or not web search is available.
         */
        boolean isWebSearchAvailable();
    }

    private Context mContext;
    private ActionHandler mActionHandler;
    private final boolean mIncognito;
    private boolean mEditable;

    protected SelectActionModeCallback(
            Context context, ActionHandler actionHandler, boolean incognito) {
        mContext = context;
        mActionHandler = actionHandler;
        mIncognito = incognito;
    }

    protected Context getContext() {
        return mContext;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTitle(null);
        mode.setSubtitle(null);
        mEditable = mActionHandler.isSelectionEditable();
        createActionMenu(mode, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        boolean isEditableNow = mActionHandler.isSelectionEditable();
        if (mEditable != isEditableNow) {
            mEditable = isEditableNow;
            menu.clear();
            createActionMenu(mode, menu);
            return true;
        }
        return false;
    }

    private void createActionMenu(ActionMode mode, Menu menu) {
        TypedArray styledAttributes = getContext().obtainStyledAttributes(
                R.style.ContentActionBar, ACTION_MODE_ATTRS);

        menu.add(Menu.NONE, ID_SELECTALL, Menu.NONE, android.R.string.selectAll).
            setAlphabeticShortcut('a').
            setIcon(styledAttributes.getResourceId(SELECT_ALL_ATTR_INDEX, 0)).
            setShowAsAction(
                    MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        if (mEditable) {
            menu.add(Menu.NONE, ID_CUT, Menu.NONE, android.R.string.cut).
            setIcon(styledAttributes.getResourceId(CUT_ATTR_INDEX, 0)).
            setAlphabeticShortcut('x').
            setShowAsAction(
                    MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }

        menu.add(Menu.NONE, ID_COPY, Menu.NONE, android.R.string.copy).
            setIcon(styledAttributes.getResourceId(COPY_ATTR_INDEX, 0)).
            setAlphabeticShortcut('c').
            setShowAsAction(
                    MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        if (mEditable && canPaste()) {
            menu.add(Menu.NONE, ID_PASTE, Menu.NONE, android.R.string.paste).
                setIcon(styledAttributes.getResourceId(PASTE_ATTR_INDEX, 0)).
                setAlphabeticShortcut('v').
                setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }

        if (!mEditable) {
            if (mActionHandler.isShareAvailable()) {
                menu.add(Menu.NONE, ID_SHARE, Menu.NONE, R.string.actionbar_share).
                    setIcon(styledAttributes.getResourceId(SHARE_ATTR_INDEX, 0)).
                    setShowAsAction(
                            MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            }

            if (!mIncognito && mActionHandler.isWebSearchAvailable()) {
                menu.add(Menu.NONE, ID_SEARCH, Menu.NONE, R.string.actionbar_web_search).
                    setIcon(styledAttributes.getResourceId(WEB_SEARCH_ATTR_INDEX, 0)).
                    setShowAsAction(
                            MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            }
        }

        styledAttributes.recycle();
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch(item.getItemId()) {
            case ID_SELECTALL:
                mActionHandler.selectAll();
                break;
            case ID_CUT:
                mActionHandler.cut();
                break;
            case ID_COPY:
                mActionHandler.copy();
                mode.finish();
                break;
            case ID_PASTE:
                mActionHandler.paste();
                break;
            case ID_SHARE:
                mActionHandler.share();
                mode.finish();
                break;
            case ID_SEARCH:
                mActionHandler.search();
                mode.finish();
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionHandler.onDestroyActionMode();
    }

    private boolean canPaste() {
        ClipboardManager clipMgr = (ClipboardManager)
                getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        return clipMgr.hasPrimaryClip();
    }
}
