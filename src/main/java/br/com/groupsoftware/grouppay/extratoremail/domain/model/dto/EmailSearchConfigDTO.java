package br.com.groupsoftware.grouppay.extratoremail.domain.model.dto;

import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ConfigurationEmailType;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.CryptographyType;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ProtocolType;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.RedirectStatusTestType;
import lombok.*;

import java.time.ZonedDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class EmailSearchConfigDTO {
    private String uuid;
    private boolean active;
    private ConfigurationEmailType configurationEmail;
    private String email;
    private String password;
    private ProtocolType protocol;
    private String server;
    private Integer port;
    private ZonedDateTime testSendDateTime;
    private RedirectStatusTestType testSendStatus;
    private String testSendEmail;
    private CryptographyType cryptography;
}
