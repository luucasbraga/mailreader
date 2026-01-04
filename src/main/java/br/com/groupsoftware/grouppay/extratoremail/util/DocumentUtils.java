package br.com.groupsoftware.grouppay.extratoremail.util;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.AiPlanType;
import lombok.experimental.UtilityClass;

import java.util.Objects;

@UtilityClass
public class DocumentUtils {

    public static boolean isAiUser(Document document) {
        if (Objects.nonNull(document.getClientGroup())) {
            return document.getClientGroup().isAiUser();
        }
        else {
            return document.getCompany().getClientGroup().isAiUser();
        }
    }

    public static AiPlanType getAiPlanType(Document document) {
        if (Objects.nonNull(document.getClientGroup())) {
            return document.getClientGroup().getAiPlanType();
        }
        else {
            return document.getCompany().getClientGroup().getAiPlanType();
        }
    }

}
