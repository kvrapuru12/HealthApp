package com.healthapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Unified cycle sync recommendations for all phases")
public class CycleSyncUnifiedResponse {

    private CyclePhaseRecommendation menstrual;
    private CyclePhaseRecommendation follicular;
    private CyclePhaseRecommendation ovulation;
    private CyclePhaseRecommendation luteal;

    public CyclePhaseRecommendation getMenstrual() {
        return menstrual;
    }

    public void setMenstrual(CyclePhaseRecommendation menstrual) {
        this.menstrual = menstrual;
    }

    public CyclePhaseRecommendation getFollicular() {
        return follicular;
    }

    public void setFollicular(CyclePhaseRecommendation follicular) {
        this.follicular = follicular;
    }

    public CyclePhaseRecommendation getOvulation() {
        return ovulation;
    }

    public void setOvulation(CyclePhaseRecommendation ovulation) {
        this.ovulation = ovulation;
    }

    public CyclePhaseRecommendation getLuteal() {
        return luteal;
    }

    public void setLuteal(CyclePhaseRecommendation luteal) {
        this.luteal = luteal;
    }

    public static class CyclePhaseRecommendation {
        private String phaseName;
        private String days;
        private String subtitle;
        private Integer energyLevel;
        private Move move;
        private EatToday eatToday;
        private List<String> feel = new ArrayList<>();
        private List<AvoidDetailed> avoidDetailed = new ArrayList<>();
        private String tip;
        private String digestionNote;
        private Theme theme;

        public String getPhaseName() {
            return phaseName;
        }

        public void setPhaseName(String phaseName) {
            this.phaseName = phaseName;
        }

        public String getDays() {
            return days;
        }

        public void setDays(String days) {
            this.days = days;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public void setSubtitle(String subtitle) {
            this.subtitle = subtitle;
        }

        public Integer getEnergyLevel() {
            return energyLevel;
        }

        public void setEnergyLevel(Integer energyLevel) {
            this.energyLevel = energyLevel;
        }

        public Move getMove() {
            return move;
        }

        public void setMove(Move move) {
            this.move = move;
        }

        public EatToday getEatToday() {
            return eatToday;
        }

        public void setEatToday(EatToday eatToday) {
            this.eatToday = eatToday;
        }

        public List<String> getFeel() {
            return feel;
        }

        public void setFeel(List<String> feel) {
            this.feel = feel;
        }

        public List<AvoidDetailed> getAvoidDetailed() {
            return avoidDetailed;
        }

        public void setAvoidDetailed(List<AvoidDetailed> avoidDetailed) {
            this.avoidDetailed = avoidDetailed;
        }

        public String getTip() {
            return tip;
        }

        public void setTip(String tip) {
            this.tip = tip;
        }

        public String getDigestionNote() {
            return digestionNote;
        }

        public void setDigestionNote(String digestionNote) {
            this.digestionNote = digestionNote;
        }

        public Theme getTheme() {
            return theme;
        }

        public void setTheme(Theme theme) {
            this.theme = theme;
        }
    }

    public static class Move {
        private String title;
        private String intensity;
        private String sessionHint;
        private String main;
        private String mainDetail;
        private String extra;
        private String extraDetail;
        private Boolean strengthFocus;
        private String note;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getIntensity() {
            return intensity;
        }

        public void setIntensity(String intensity) {
            this.intensity = intensity;
        }

        public String getSessionHint() {
            return sessionHint;
        }

        public void setSessionHint(String sessionHint) {
            this.sessionHint = sessionHint;
        }

        public String getMain() {
            return main;
        }

        public void setMain(String main) {
            this.main = main;
        }

        public String getMainDetail() {
            return mainDetail;
        }

        public void setMainDetail(String mainDetail) {
            this.mainDetail = mainDetail;
        }

        public String getExtra() {
            return extra;
        }

        public void setExtra(String extra) {
            this.extra = extra;
        }

        public String getExtraDetail() {
            return extraDetail;
        }

        public void setExtraDetail(String extraDetail) {
            this.extraDetail = extraDetail;
        }

        public Boolean getStrengthFocus() {
            return strengthFocus;
        }

        public void setStrengthFocus(Boolean strengthFocus) {
            this.strengthFocus = strengthFocus;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }

    public static class EatToday {
        private Categories categories;
        private List<String> digestiveSupport = new ArrayList<>();
        private List<String> prebioticFoods = new ArrayList<>();
        private List<String> probioticFoods = new ArrayList<>();
        private SeedCycling seedCycling;

        public Categories getCategories() {
            return categories;
        }

        public void setCategories(Categories categories) {
            this.categories = categories;
        }

        public List<String> getDigestiveSupport() {
            return digestiveSupport;
        }

        public void setDigestiveSupport(List<String> digestiveSupport) {
            this.digestiveSupport = digestiveSupport;
        }

        public List<String> getPrebioticFoods() {
            return prebioticFoods;
        }

        public void setPrebioticFoods(List<String> prebioticFoods) {
            this.prebioticFoods = prebioticFoods;
        }

        public List<String> getProbioticFoods() {
            return probioticFoods;
        }

        public void setProbioticFoods(List<String> probioticFoods) {
            this.probioticFoods = probioticFoods;
        }

        public SeedCycling getSeedCycling() {
            return seedCycling;
        }

        public void setSeedCycling(SeedCycling seedCycling) {
            this.seedCycling = seedCycling;
        }
    }

    public static class Categories {
        private List<String> carbs = new ArrayList<>();
        private List<String> protein = new ArrayList<>();
        private List<String> fats = new ArrayList<>();
        private List<String> greens = new ArrayList<>();

        public List<String> getCarbs() {
            return carbs;
        }

        public void setCarbs(List<String> carbs) {
            this.carbs = carbs;
        }

        public List<String> getProtein() {
            return protein;
        }

        public void setProtein(List<String> protein) {
            this.protein = protein;
        }

        public List<String> getFats() {
            return fats;
        }

        public void setFats(List<String> fats) {
            this.fats = fats;
        }

        public List<String> getGreens() {
            return greens;
        }

        public void setGreens(List<String> greens) {
            this.greens = greens;
        }
    }

    public static class SeedCycling {
        private List<String> main = new ArrayList<>();
        private List<String> optionalAddons = new ArrayList<>();

        public List<String> getMain() {
            return main;
        }

        public void setMain(List<String> main) {
            this.main = main;
        }

        public List<String> getOptionalAddons() {
            return optionalAddons;
        }

        public void setOptionalAddons(List<String> optionalAddons) {
            this.optionalAddons = optionalAddons;
        }
    }

    public static class AvoidDetailed {
        private String item;
        private String reason;

        public String getItem() {
            return item;
        }

        public void setItem(String item) {
            this.item = item;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class Theme {
        private String accent;
        private String background;

        public String getAccent() {
            return accent;
        }

        public void setAccent(String accent) {
            this.accent = accent;
        }

        public String getBackground() {
            return background;
        }

        public void setBackground(String background) {
            this.background = background;
        }
    }
}
