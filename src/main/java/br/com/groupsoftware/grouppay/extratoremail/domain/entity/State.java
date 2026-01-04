package br.com.groupsoftware.grouppay.extratoremail.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * Entity that represents a state in the system.
 * <p>
 * Stores information related to the state, such as code, name, abbreviation, location, and region.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Entity
@Table(name = "tb_state")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class State implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "CODE_UF", nullable = false)
    private Integer codeUf;

    @Column(name = "UF", nullable = false, length = 2)
    private String uf;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "LATITUDE", nullable = false)
    private Float latitude;

    @Column(name = "LONGITUDE", nullable = false)
    private Float longitude;

    @Column(name = "REGION", nullable = false, length = 12)
    private String region;

    @Override
    public String toString() {
        return "State{" +
                "codeUf=" + codeUf +
                ", uf='" + uf + '\'' +
                ", name='" + name + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", region='" + region + '\'' +
                '}';
    }
}