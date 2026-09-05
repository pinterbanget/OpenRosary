package com.openrosary.app;

import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Markdown (.md) devotion files into structured prayer steps with vibration cues.
 *
 * Supported Markdown format:
 *   # Title
 *   Subtitle description
 *   ---
 *   ### Section Header
 *   ## [Nx] Prayer Label [*|**|***]
 *   Prayer text body or @Macro...
 *
 * Macros:
 *   @SignOfCross, @OurFather, @HailMary, @GloryBe, @Creed, @Fatima, @HailHolyQueen
 */
public final class DevotionParser {
    private static final String TAG = "DevotionParser";

    public static final class ParsedDevotion {
        public final String title;
        public final String subtitle;
        public final List<DevotionState.Step> steps;

        public ParsedDevotion(String title, String subtitle, List<DevotionState.Step> steps) {
            this.title = title != null ? title : "Custom Devotion";
            this.subtitle = subtitle != null ? subtitle : "";
            this.steps = steps != null ? steps : new ArrayList<>();
        }
    }

    private static final Pattern REPEAT_PATTERN = Pattern.compile("^(\\d+)[xX]\\s+(.*)$");
    private static final Pattern VIBE_PATTERN = Pattern.compile("\\[([*~_\\w:]+)\\]");

    public static ParsedDevotion parseAsset(Context context, String assetPath) {
        if (context == null || assetPath == null) return null;
        try (InputStream is = context.getAssets().open(assetPath)) {
            return parseStream(context, is);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load devotion asset: " + assetPath, e);
            return null;
        }
    }

    public static ParsedDevotion parseFile(Context context, File file) {
        if (file == null || !file.exists()) return new ParsedDevotion("Devotion", "", new ArrayList<>());
        try (FileInputStream fis = new FileInputStream(file)) {
            return parseStream(context, fis);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load devotion file: " + file.getPath(), e);
            return new ParsedDevotion("Devotion", "", new ArrayList<>());
        }
    }

    public static ParsedDevotion parseStream(Context context, InputStream is) {
        if (is == null) return new ParsedDevotion("Devotion", "", new ArrayList<>());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return parse(context, sb.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to read stream", e);
            return new ParsedDevotion("Devotion", "", new ArrayList<>());
        }
    }

    public static ParsedDevotion parse(Context context, String markdown) {
        if (context != null) {
            RosaryPrayers.initialize(context);
        }
        String title = "Custom Devotion";
        String subtitle = "";
        String currentSection = "";

        List<DevotionState.Step> steps = new ArrayList<>();
        if (markdown == null || markdown.trim().isEmpty()) {
            return new ParsedDevotion(title, subtitle, steps);
        }

        String[] lines = markdown.split("\\r?\\n");
        int i = 0;

        // Parse title and subtitle
        while (i < lines.length) {
            String line = lines[i].trim();
            if (line.startsWith("# ")) {
                title = line.substring(2).trim();
                i++;
                while (i < lines.length && lines[i].trim().isEmpty()) i++;
                if (i < lines.length && !lines[i].trim().startsWith("#") && !lines[i].trim().startsWith("---")) {
                    subtitle = lines[i].trim();
                    i++;
                }
                break;
            } else if (!line.isEmpty() && !line.startsWith("---")) {
                break;
            }
            i++;
        }

        String currentLabel = null;
        String currentVibe = "*";
        int currentCount = 1;
        StringBuilder currentText = new StringBuilder();

        while (i < lines.length) {
            String line = lines[i];
            String trimmed = line.trim();

            if (trimmed.startsWith("### ")) {
                // Section header
                if (currentLabel != null) {
                    addSteps(context, steps, currentSection, currentLabel, currentText.toString().trim(), currentCount, currentVibe);
                    currentLabel = null;
                    currentText.setLength(0);
                }
                currentSection = trimmed.substring(4).trim();
            } else if (trimmed.startsWith("## ")) {
                // Step header
                if (currentLabel != null) {
                    addSteps(context, steps, currentSection, currentLabel, currentText.toString().trim(), currentCount, currentVibe);
                    currentLabel = null;
                    currentText.setLength(0);
                }

                String header = trimmed.substring(3).trim();
                currentVibe = "*";
                Matcher vm = VIBE_PATTERN.matcher(header);
                if (vm.find()) {
                    currentVibe = vm.group(1);
                    header = header.replace(vm.group(0), "").trim();
                }

                currentCount = 1;
                Matcher rm = REPEAT_PATTERN.matcher(header);
                if (rm.matches()) {
                    try {
                        currentCount = Integer.parseInt(rm.group(1));
                        header = rm.group(2).trim();
                    } catch (NumberFormatException ignored) {}
                }

                currentLabel = header;
            } else if (trimmed.equals("---")) {
                // Divider
            } else {
                if (currentLabel != null) {
                    if (currentText.length() > 0) {
                        currentText.append("\n");
                    }
                    currentText.append(line);
                }
            }
            i++;
        }

        if (currentLabel != null) {
            addSteps(context, steps, currentSection, currentLabel, currentText.toString().trim(), currentCount, currentVibe);
        }

        return new ParsedDevotion(title, subtitle, steps);
    }

    private static String resolveMacroText(String macroKey) {
        if (macroKey == null) return null;
        String m = macroKey.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
        switch (m) {
            case "ourfather":
            case "bapakami":
            case "pater":
            case "paternoster":
                return RosaryPrayers.getOurFather();

            case "hailmary":
            case "salammaria":
            case "ave":
            case "avemaria":
                return RosaryPrayers.getHailMary();

            case "glorybe":
            case "kemuliaan":
            case "gloria":
            case "gloriapatri":
                return RosaryPrayers.getGloryBe();

            case "signofcross":
            case "signofthecross":
            case "tandasalib":
                return RosaryPrayers.getSignOfCross();

            case "creed":
            case "apostlescreed":
            case "syahadat":
            case "credo":
                return RosaryPrayers.getApostlesCreed();

            case "fatima":
            case "fatimaprayer":
                return RosaryPrayers.getFatimaPrayer();

            case "hailholyqueen":
            case "salveregina":
                return RosaryPrayers.getHailHolyQueen();

            default:
                return null;
        }
    }

    private static String resolveMacroTitle(Context context, String macroKey) {
        if (context == null || macroKey == null) return null;
        String m = macroKey.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
        switch (m) {
            case "ourfather":
            case "bapakami":
            case "pater":
            case "paternoster":
                return context.getString(R.string.title_our_father);

            case "hailmary":
            case "salammaria":
            case "ave":
            case "avemaria":
                return context.getString(R.string.title_hail_mary);

            case "glorybe":
            case "kemuliaan":
            case "gloria":
            case "gloriapatri":
                return context.getString(R.string.title_glory_be);

            case "signofcross":
            case "signofthecross":
            case "tandasalib":
                return context.getString(R.string.title_sign_of_cross);

            case "creed":
            case "apostlescreed":
            case "syahadat":
            case "credo":
                return context.getString(R.string.title_apostles_creed);

            case "fatima":
            case "fatimaprayer":
                return context.getString(R.string.title_fatima_prayer);

            case "hailholyqueen":
            case "salveregina":
                return context.getString(R.string.title_hail_holy_queen);

            default:
                return null;
        }
    }

    private static void addSteps(Context context, List<DevotionState.Step> steps, String section, String rawLabel, String text, int count, String vibe) {
        String resolvedLabel = rawLabel != null ? rawLabel.trim() : "";
        String resolvedText = text != null ? text.trim() : "";

        // 1. If label is a macro (e.g. "## @SignOfCross" or "## @OurFather")
        if (resolvedLabel.startsWith("@")) {
            String macroKey = resolvedLabel.substring(1).trim();
            String title = resolveMacroTitle(context, macroKey);
            if (title != null) resolvedLabel = title;
            if (resolvedText.isEmpty() || resolvedText.startsWith("@")) {
                String body = resolveMacroText(macroKey);
                if (body != null) resolvedText = body;
            }
        }

        // 2. If body text is a macro (e.g. line is "@SignOfCross")
        if (resolvedText.startsWith("@")) {
            String macroKey = resolvedText.substring(1).trim();
            String body = resolveMacroText(macroKey);
            if (body != null) resolvedText = body;
        }

        // 3. If body text is empty, check if label matches any standard prayer name to auto-fill
        if (resolvedText.isEmpty() && context != null) {
            String macroFromLabel = resolveMacroText(resolvedLabel);
            if (macroFromLabel != null) {
                resolvedText = macroFromLabel;
            }
        }

        // 4. Resolve any embedded @macros within the text lines
        if (resolvedText.contains("@")) {
            String[] commonMacros = {
                "@SignOfCross", "@signofcross", "@sign_of_cross", "@signofthecross", "@tanda_salib", "@tandasalib",
                "@OurFather", "@ourfather", "@our_father", "@bapakami", "@bapa_kami",
                "@HailMary", "@hailmary", "@hail_mary", "@salammaria", "@salam_maria",
                "@GloryBe", "@glorybe", "@glory_be", "@kemuliaan",
                "@Creed", "@creed", "@apostlescreed", "@apostles_creed", "@ApostlesCreed", "@syahadat",
                "@Fatima", "@fatima", "@fatimaprayer", "@fatima_prayer",
                "@HailHolyQueen", "@hailholyqueen", "@hail_holy_queen", "@salveregina"
            };
            for (String cm : commonMacros) {
                if (resolvedText.contains(cm)) {
                    String rep = resolveMacroText(cm.substring(1));
                    if (rep != null) {
                        resolvedText = resolvedText.replace(cm, rep);
                    }
                }
            }
        }

        if (count > 1) {
            for (int k = 1; k <= count; k++) {
                String numberedLabel = resolvedLabel + " (" + k + "/" + count + ")";
                steps.add(new DevotionState.Step(section, numberedLabel, resolvedText, vibe));
            }
        } else {
            steps.add(new DevotionState.Step(section, resolvedLabel, resolvedText, vibe));
        }
    }
}
