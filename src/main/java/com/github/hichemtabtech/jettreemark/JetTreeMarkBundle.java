package com.github.hichemtabtech.jettreemark;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

/**
 * Message bundle for the plugin.
 */
public final class JetTreeMarkBundle extends DynamicBundle {
    @NonNls
    private static final String BUNDLE = "messages.JetTreeMarkBundle";
    private static final JetTreeMarkBundle INSTANCE = new JetTreeMarkBundle();

    private JetTreeMarkBundle() {
        super(BUNDLE);
    }

    /**
     * Gets a message from the bundle.
     *
     * @param key    the key of the message
     * @param params the parameters for the message
     * @return the message
     */
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
        return INSTANCE.getMessage(key, params);
    }
}