package org.telegram.ui;

/** Only the provider type is needed by the module's reflective openPhoto call. */
public final class PhotoViewer {
    public interface PhotoViewerProvider {}
    public static class EmptyPhotoViewerProvider implements PhotoViewerProvider {}
}
