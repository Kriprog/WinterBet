package com.winternship.bettingDataProcessor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Match {
    private UUID matchId;
    private BigDecimal rateValueA;
    private BigDecimal rateValueB;
    private BigDecimal rateValueDraw;
    private String winningSide;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Match match = (Match) obj;
        return Objects.equals(matchId, match.matchId);
    }

}
