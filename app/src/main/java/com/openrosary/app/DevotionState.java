package com.openrosary.app;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

/** Offline native equivalent of the devotion flows in openrosary-web with symbolic vibration support. */
public final class DevotionState {
    public static final class Step {
        public final String section, label, text, vibration;
        public Step(String section, String label, String text, String vibration) {
            this.section = section; this.label = label; this.text = text;
            this.vibration = vibration != null ? vibration : "*";
        }
    }

    private final Context context;
    private final String type;
    private final List<Step> steps = new ArrayList<>();
    private int index = 0;

    public DevotionState(Context context, String type) {
        this.context = context;
        this.type = type;
        RosaryPrayers.initialize(context);
        build();
    }

    public DevotionState(Context context, DevotionParser.ParsedDevotion devotion) {
        this.context = context;
        this.type = devotion != null ? devotion.title : "Custom Devotion";
        RosaryPrayers.initialize(context);
        if (devotion != null) {
            this.steps.addAll(devotion.steps);
        }
    }

    private String s(int id) { return context.getString(id); }
    private void add(String section, String label, String text) { add(section, label, text, "*"); }
    private void add(String section, String label, String text, String vibration) {
        steps.add(new Step(section, label, text, vibration));
    }
    private void repeat(String section, String label, String text, int count) {
        repeat(section, label, text, count, "*");
    }
    private void repeat(String section, String label, String text, int count, String vibration) {
        for (int i = 1; i <= count; i++) add(section, label + " (" + i + "/" + count + ")", text, vibration);
    }

    private void build() {
        String lang = isIndonesian() ? "in" : "en";
        String assetPath = "devotions/" + lang + "/" + type + ".md";
        DevotionParser.ParsedDevotion parsed = DevotionParser.parseAsset(context, assetPath);
        if (parsed == null || parsed.steps.isEmpty()) {
            assetPath = "devotions/en/" + type + ".md";
            parsed = DevotionParser.parseAsset(context, assetPath);
        }
        if (parsed != null && !parsed.steps.isEmpty()) {
            this.steps.addAll(parsed.steps);
            return;
        }
        if ("our-father-77".equals(type)) {
            add("", s(R.string.title_sign_of_cross), RosaryPrayers.getSignOfCross(), "***");
            add("", s(R.string.opening_petitions_title), s(R.string.opening_petitions_text), "**");
            add("", s(R.string.personal_intention_title), s(R.string.personal_intention_text), "**");
            add("", s(R.string.title_apostles_creed), RosaryPrayers.getApostlesCreed(), "*");
            repeat("", s(R.string.title_our_father), RosaryPrayers.getOurFather(), 77, "*");
            add("", s(R.string.title_glory_be), RosaryPrayers.getGloryBe(), "**");
            add("", s(R.string.title_sign_of_cross), RosaryPrayers.getSignOfCross(), "***");
            return;
        }
        if ("divine-mercy".equals(type)) {
            add("", s(R.string.title_sign_of_cross), RosaryPrayers.getSignOfCross(), "***");
            add("", s(R.string.title_our_father), RosaryPrayers.getOurFather(), "**");
            add("", s(R.string.title_hail_mary), RosaryPrayers.getHailMary(), "*");
            add("", s(R.string.title_apostles_creed), RosaryPrayers.getApostlesCreed(), "*");
            for (int d = 1; d <= 5; d++) {
                String section = (isIndonesian() ? "Dekade " : "Decade ") + d;
                add(section, s(R.string.eternal_father_title), s(R.string.eternal_father_text), "**");
                repeat(section, s(R.string.sorrowful_passion_title), s(R.string.sorrowful_passion_text), 10, "*");
            }
            repeat("", s(R.string.holy_god_title), s(R.string.holy_god_text), 3, "***");
            return;
        }
        String[][] moments = "franciscan-crown".equals(type) ? joys() : sorrows();
        int hailCount = "franciscan-crown".equals(type) ? 10 : 7;
        String sectionWord = isIndonesian()
                ? ("franciscan-crown".equals(type) ? "Sukacita " : "Dukacita ")
                : ("franciscan-crown".equals(type) ? "Joy " : "Sorrow ");
        for (int i = 0; i < moments.length; i++) {
            String section = sectionWord + (i + 1);
            add(section, section, moments[i][0] + "\n\n" + moments[i][1], "***");
            add(section, s(R.string.title_our_father), RosaryPrayers.getOurFather(), "**");
            repeat(section, s(R.string.title_hail_mary), RosaryPrayers.getHailMary(), hailCount, "*");
            if ("franciscan-crown".equals(type)) add(section, s(R.string.title_glory_be), RosaryPrayers.getGloryBe(), "**");
        }
        if ("franciscan-crown".equals(type)) {
            repeat(isIndonesian() ? "Menghormati Usia Maria" : "In Honor of Mary’s Life", s(R.string.title_hail_mary), RosaryPrayers.getHailMary(), 2, "*");
        } else {
            repeat(isIndonesian() ? "Menghormati Air Mata Maria" : "In Honor of Mary’s Tears", s(R.string.title_hail_mary), RosaryPrayers.getHailMary(), 3, "*");
        }
        String intentions = isIndonesian() ? "Intensi Bapa Suci" : "Holy Father’s Intentions";
        add(intentions, s(R.string.title_our_father), RosaryPrayers.getOurFather(), "**");
        add(intentions, s(R.string.title_hail_mary), RosaryPrayers.getHailMary(), "*");
        add(intentions, s(R.string.title_glory_be), RosaryPrayers.getGloryBe(), "**");
        if (!"franciscan-crown".equals(type)) repeat("", isIndonesian() ? "Doa Penutup" : "Closing Invocation", s(R.string.closing_invocation_text), 3, "***");
    }

    private boolean isIndonesian() {
        String language = context.getResources().getConfiguration().getLocales().get(0).getLanguage();
        return "in".equals(language) || "id".equals(language);
    }
    private String[][] sorrows() {
        return isIndonesian() ? new String[][]{
                {"Nubuat Simeon", "Lukas 2:34–35"}, {"Pelarian ke Mesir", "Matius 2:13–15"},
                {"Yesus Hilang di Bait Allah", "Lukas 2:41–52"}, {"Maria Berjumpa dengan Yesus di Jalan Salib", "Lukas 23:26–31"},
                {"Yesus Disalibkan dan Wafat", "Yohanes 19:25–30"}, {"Maria Menerima Jenazah Yesus", "Yohanes 19:38–40"},
                {"Yesus Dimakamkan", "Yohanes 19:41–42"}
        } : new String[][]{
                {"The Prophecy of Simeon", "Luke 2:34–35"}, {"The Flight into Egypt", "Matthew 2:13–15"},
                {"The Loss of Jesus in the Temple", "Luke 2:41–52"}, {"Mary Meets Jesus on the Way to Calvary", "Luke 23:26–31"},
                {"The Crucifixion and Death of Jesus", "John 19:25–30"}, {"Mary Receives Jesus’ Body", "John 19:38–40"},
                {"Jesus Is Laid in the Tomb", "John 19:41–42"}
        };
    }
    private String[][] joys() {
        return isIndonesian() ? new String[][]{
                {"Kabar Sukacita", "Lukas 1:26–38"}, {"Kunjungan Maria kepada Elisabet", "Lukas 1:39–56"},
                {"Kelahiran Yesus dan Penyembahan Orang Majus", "Lukas 2:1–20; Matius 2:1–12"}, {"Yesus Dipersembahkan di Bait Allah", "Lukas 2:22–38"},
                {"Yesus Ditemukan di Bait Allah", "Lukas 2:41–52"}, {"Kebangkitan Yesus", "Lukas 24:1–12"},
                {"Maria Diangkat ke Surga", "Wahyu 12:1"}
        } : new String[][]{
                {"The Annunciation", "Luke 1:26–38"}, {"The Visitation", "Luke 1:39–56"},
                {"The Birth of Jesus and Adoration of the Magi", "Luke 2:1–20; Matthew 2:1–12"}, {"The Presentation of Jesus in the Temple", "Luke 2:22–38"},
                {"The Finding of Jesus in the Temple", "Luke 2:41–52"}, {"The Resurrection of Jesus", "Luke 24:1–12"},
                {"The Assumption of Mary", "Revelation 12:1"}
        };
    }

    public void next() { if (index < steps.size()) index++; }
    public void previous() { if (index > 0) index--; }
    public boolean isComplete() { return index >= steps.size(); }
    public int progress() { return Math.min(index + 1, steps.size()); }
    public int max() { return steps.size(); }
    public String section() { return isComplete() ? "" : steps.get(index).section; }
    public String label() { return isComplete() ? s(R.string.complete) : steps.get(index).label; }
    public String text() { return isComplete() ? s(R.string.prayer_complete) : steps.get(index).text; }
    public String vibration() { return isComplete() ? "***" : steps.get(index).vibration; }
}
