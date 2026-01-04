package br.com.groupsoftware.grouppay.extratoremail.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * Entity that represents a municipality in the system.
 * <p>
 * Stores information related to the municipality, such as IBGE code, name, location, among others.
 * It also has a relationship with the {@link State} entity, which represents the state to which the municipality belongs.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Entity
@Table(name = "tb_city")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class City implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "IBGE_CODE", nullable = false)
    private Long ibgeCode;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "LATITUDE", nullable = false)
    private Float latitude;

    @Column(name = "LONGITUDE", nullable = false)
    private Float longitude;

    @Column(name = "CAPITAL", nullable = false)
    private Boolean capital;

    @Column(name = "SIAFI_ID", nullable = false, unique = true, length = 4)
    private String siafiId;

    @Column(name = "AREA_CODE", nullable = false)
    private Integer areaCode;

    @Column(name = "TIME_ZONE", nullable = false, length = 32)
    private String timeZone;

    @ManyToOne
    @JoinColumn(name = "STATE_CODE_UF", nullable = false)
    private State state;

    @Override
    public String toString() {
        return "City{" +
                "ibgeCode=" + ibgeCode +
                ", name='" + name + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", capital=" + capital +
                ", siafiId='" + siafiId + '\'' +
                ", areaCode=" + areaCode +
                ", timeZone='" + timeZone + '\'' +
                ", state=" + state +
                '}';
    }
}
