package com.rpstudio.benewallet.feature.wallet.data

import java.time.ZoneId

/** Time zone the BFF uses to interpret `from`/`to` days. Keep in sync with the BFF. */
internal val bffZone: ZoneId = ZoneId.of("America/Sao_Paulo")
