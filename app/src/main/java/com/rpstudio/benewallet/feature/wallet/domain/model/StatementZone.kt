package com.rpstudio.benewallet.feature.wallet.domain.model

import java.time.ZoneId

/** Zone in which calendar days are defined for statement filters and grouping. Must match the BFF. */
internal val statementZone: ZoneId = ZoneId.of("America/Sao_Paulo")
