package com.mahghuuuls.jass.gameplay;

/**
 * A weight source whose per-piece values item tooltips can preview (REQ-058): the client gets its
 * override lines and computes each piece the same way. Elenai Dodge 2 implements it.
 */
public interface WeightPreviewSource {

    /** True while this source gives the Effective Weight. */
    boolean previewActive();

    /** The source's per-item override lines, as its own configuration writes them. */
    String[] previewOverrideLines();
}
