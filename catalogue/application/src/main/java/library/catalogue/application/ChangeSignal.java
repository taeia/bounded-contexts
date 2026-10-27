package library.catalogue.application;

/**
 * Tells whoever is listening that the catalogue changed. A driven port with one
 * method and no arguments, rung by the one use case that changes anything, as in the
 * lending context. The lending context has an interface of the same name and shape,
 * and the two are two interfaces: each context declares what it needs, and neither
 * can see the other's. The window answers both with the same lambda, which is a fact
 * about the window, not about the contexts.
 */
public interface ChangeSignal {

    void changed();
}
