package net.revilodev.boundless.client.editor;

import net.revilodev.boundless.client.editor.QuestEditorModels.EditorType;

// editor state signatures
public final class QuestEditorStateSignatures {
    private QuestEditorStateSignatures() {
    }

    // signature
    public static String signature(EditorType editorType, SignatureFields fields) {
        if (fields == null) fields = new SignatureFields();
        return switch (editorType) {
            case PACK_CREATE -> "pack|" + safe(fields.packName)
                    + "|" + safe(fields.packNamespace);
            case PACK_OPTIONS -> "pack-options|" + safe(fields.packName)
                    + "|" + safe(fields.packIconPath)
                    + "|" + safe(fields.packDescription);
            case CATEGORY -> "category|" + safe(fields.categoryId)
                    + "|" + safe(fields.categoryName)
                    + "|" + safe(fields.categoryIcon)
                    + "|" + safe(fields.categoryDependency)
                    + "|" + fields.categoryAutoComplete;
            case SUBCATEGORY -> "subcategory|" + safe(fields.subCategoryId)
                    + "|" + safe(fields.subCategoryParent)
                    + "|" + safe(fields.subCategoryName)
                    + "|" + safe(fields.subCategoryIcon)
                    + "|" + fields.subCategoryDefaultOpen;
            case QUEST -> "quest|" + safe(fields.questId)
                    + "|" + safe(fields.questOrderToken)
                    + "|" + safe(fields.questName)
                    + "|" + safe(fields.questIcon)
                    + "|" + safe(fields.questDescription)
                    + "|" + safe(fields.questCategory)
                    + "|" + safe(fields.questSubCategory)
                    + "|" + safe(fields.questDependencies)
                    + "|" + fields.questDependencyLock
                    + "|" + fields.questOptional
                    + "|" + fields.questRepeatable
                    + "|" + fields.questAutoComplete
                    + "|" + fields.questHiddenUnderDependency
                    + "|" + safe(fields.questCompletion)
                    + "|" + safe(fields.questReward)
                    + "|" + safe(fields.loadedQuestType);
            case NONE -> "";
        };
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    // signature fields
    public static final class SignatureFields {
        public String packName = "";
        public String packNamespace = "";
        public String packIconPath = "";
        public String packDescription = "";
        public String categoryId = "";
        public String categoryName = "";
        public String categoryIcon = "";
        public String categoryDependency = "";
        public boolean categoryAutoComplete;
        public String subCategoryId = "";
        public String subCategoryParent = "";
        public String subCategoryName = "";
        public String subCategoryIcon = "";
        public boolean subCategoryDefaultOpen;
        public String questId = "";
        public String questOrderToken = "";
        public String questName = "";
        public String questIcon = "";
        public String questDescription = "";
        public String questCategory = "";
        public String questSubCategory = "";
        public String questDependencies = "";
        public boolean questDependencyLock;
        public boolean questOptional;
        public boolean questRepeatable;
        public boolean questAutoComplete;
        public boolean questHiddenUnderDependency;
        public String questCompletion = "";
        public String questReward = "";
        public String loadedQuestType = "";
    }
}
