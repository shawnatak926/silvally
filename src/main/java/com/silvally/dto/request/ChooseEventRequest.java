package com.silvally.dto.request;

import jakarta.validation.constraints.AssertTrue;
import java.util.List;

public record ChooseEventRequest(String choiceCode, List<String> choiceCodes, java.util.UUID questionId, String answer) {
    public ChooseEventRequest(String choiceCode, List<String> choiceCodes) { this(choiceCode, choiceCodes, null, null); }
    public ChooseEventRequest(String choiceCode) { this(choiceCode, null); }
    @AssertTrue(message = "choiceCode 또는 choiceCodes 중 하나만 지정해야 합니다.")
    public boolean isSelectionValid() {
        if ("ANSWER".equals(choiceCode)) return choiceCodes == null && questionId != null
                && answer != null && !answer.isBlank() && answer.length() <= 1200;
        if (questionId != null || answer != null) return false;
        if (choiceCode != null) return choiceCodes == null && !choiceCode.isBlank();
        return choiceCodes != null && !choiceCodes.isEmpty()
                && choiceCodes.stream().allMatch(code -> code != null && !code.isBlank());
    }
}
