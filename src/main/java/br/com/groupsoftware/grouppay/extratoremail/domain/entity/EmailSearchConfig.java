package br.com.groupsoftware.grouppay.extratoremail.domain.entity;

import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ConfigurationEmailType;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.CryptographyType;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ProtocolType;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.RedirectStatusTestType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Entity
@Table(name = "tb_email_search_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailSearchConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "UUID", unique = true, nullable = false)
    private String uuid;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "ACTIVE")
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(name = "CONFIGURATION_EMAIL")
    private ConfigurationEmailType configurationEmail;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "PASSWORD")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "PROTOCOL")
    private ProtocolType protocol = ProtocolType.IMAP;

    @Column(name = "SERVER")
    private String server;

    @Column(name = "PORT")
    private Integer port;

    @Column(name = "TEST_SEND_DATE_TIME")
    private ZonedDateTime testSendDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "TEST_SEND_STATUS")
    private RedirectStatusTestType testSendStatus;

    @Column(name = "TEST_SEND_EMAIL")
    private String testSendEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "CRYPTOGRAPHY")
    private CryptographyType cryptography;

    @OneToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @Column
    private String stacktrace;

    @Column(name = "OAUTH2_ENABLED")
    private Boolean oauth2Enabled = false;

    @Column(name = "OAUTH2_ACCESS_TOKEN", columnDefinition = "TEXT")
    private String oauth2AccessToken;

    @Column(name = "OAUTH2_REFRESH_TOKEN", columnDefinition = "TEXT")
    private String oauth2RefreshToken;

    @Column(name = "OAUTH2_TOKEN_EXPIRY")
    private LocalDateTime oauth2TokenExpiry;

}
