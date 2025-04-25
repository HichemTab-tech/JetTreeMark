package com.github.hichemtabtech.jettreemark.listeners;

import com.intellij.openapi.application.ApplicationActivationListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.wm.IdeFrame;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for application activation events.
 */
public class ApplicationStartupListener implements ApplicationActivationListener {

    private static final Logger LOG = Logger.getInstance(ApplicationStartupListener.class);

    @Override
    public void applicationActivated(@NotNull IdeFrame ideFrame) {
        LOG.info("JetTreeMark plugin activated");
    }
}