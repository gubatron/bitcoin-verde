package com.softwareverde.bitcoin.bip;

import com.softwareverde.util.Util;

public class Bip113 {
    // Median Time-Past As Endpoint For LockTime Calculations -- https://github.com/bitcoin/bips/blob/master/bip-0113.mediawiki
    public static Boolean isEnabled(final Long blockHeight) {
        return (Util.coalesce(blockHeight, Long.MAX_VALUE) >= 419328L); // https://www.reddit.com/r/Bitcoin/comments/4r9tiv/csv_soft_fork_has_activated_as_of_block_419328
    }

    protected Bip113() { }
}
