package com.miniframework.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Fichier reçu par une requête multipart : nom, type MIME (déclarés par le client)
 * et contenu brut. Immuable : les octets sont lus une fois par le FrontController.
 */
public final class UploadedFile {

    private final String name;
    private final String mimeType;
    private final byte[] content;

    public UploadedFile(String name, String mimeType, byte[] content) {
        this.name = name == null ? "" : name;
        this.mimeType = mimeType == null ? "" : mimeType;
        this.content = content == null ? new byte[0] : content;
    }

    public String getName() { return name; }
    public String getMimeType() { return mimeType; }
    public long getSize() { return content.length; }

    public boolean isEmpty() {
        return name.isBlank() && content.length == 0;
    }

    public void saveTo(Path destination) throws IOException {
        Objects.requireNonNull(destination, "destination");
        if (isEmpty()) {
            throw new IllegalStateException("Aucun fichier à enregistrer (aucun fichier choisi dans le formulaire).");
        }
        Path parent = destination.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(destination, content); // CREATE + TRUNCATE_EXISTING + WRITE par défaut
    }
}