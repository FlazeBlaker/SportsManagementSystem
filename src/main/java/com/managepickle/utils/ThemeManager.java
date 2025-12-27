package com.managepickle.utils;

import com.managepickle.model.AppConfig;
import javafx.scene.Scene;

public class ThemeManager {

    public static void applyTheme(Scene scene) {
        AppConfig config = ConfigManager.getConfig();

        // Generate CSS variables from config
        String themeCSS = generateThemeCSS(config);
        System.out.println("Applying Theme CSS: " + themeCSS);

        // Apply inline styles to root
        if (scene != null && scene.getRoot() != null) {
            String existingStyle = scene.getRoot().getStyle();
            if (existingStyle == null)
                existingStyle = "";

            String finalStyle = existingStyle + (existingStyle.isEmpty() ? "" : "; ") + themeCSS;

            try {
                scene.getRoot().setStyle(finalStyle);
                System.out.println("✅ Theme Applied Successfully. Root Style length: " + finalStyle.length());
                System.out.println(
                        "Preview: " + (finalStyle.length() > 100 ? finalStyle.substring(0, 100) + "..." : finalStyle));
            } catch (Exception e) {
                System.err.println("❌ Failed to set style on root node: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("❌ Scene root is NULL, cannot apply theme.");
        }
    }

    private static String generateThemeCSS(AppConfig config) {
        // Define color variables
        String colorVars = String.format(
                "-color-brand: %s; " +
                        "-color-outline: %s; " +
                        "-color-panel: %s; " +
                        "-color-navbar: %s; " +
                        "-color-text: %s; " +
                        "-color-bg: %s;",
                config.getBrandColor(),
                config.getOutlineColor(),
                config.getPanelColor(),
                config.getNavbarColor(),
                config.getTextColor(),
                config.getBackgroundColor());

        // Define gradient variables
        String gradientVars = String.format(
                " -gradient-primary: linear-gradient(135deg, %s 0%%, derive(%s, 20%%) 100%%);" +
                        " -gradient-secondary: linear-gradient(135deg, %s 0%%, derive(%s, 20%%) 100%%);" +
                        " -gradient-bg: linear-gradient(180deg, %s 0%%, %s 100%%);",
                config.getBrandColor(), config.getBrandColor(),
                config.getOutlineColor(), config.getOutlineColor(),
                config.getBackgroundColor(), config.getPanelColor());

        // CRITICAL: Use actual hex values for background, not variable references
        // JavaFX doesn't properly resolve CSS variable lookups in inline styles
        String backgroundStyle = String.format(
                " -fx-background-color: linear-gradient(to bottom, %s 0%%, %s 100%%);",
                config.getBackgroundColor(),
                config.getBackgroundColor());

        return colorVars + gradientVars + backgroundStyle;
    }
}
