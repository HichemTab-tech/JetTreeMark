package com.github.hichemtabtech.jettreemark.listeners;

import com.intellij.openapi.application.ApplicationActivationListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.wm.IdeFrame;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for application activation events.
 */
public class MyApplicationActivationListener implements ApplicationActivationListener {

    private static final Logger LOG = Logger.getInstance(MyApplicationActivationListener.class);

    @Override
    public void applicationActivated(@NotNull IdeFrame ideFrame) {
        LOG.warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.");
    }
}