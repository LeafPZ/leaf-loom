package dev.aoqia.leaf.loom.util.copygamefile;

public interface CopyGameFileProgressListener {
    CopyGameFileProgressListener NONE = new CopyGameFileProgressListener() {
        @Override
        public void onStart() {}

        @Override
        public void onProgress(long bytesTransferred, long contentLength) {}

        @Override
        public void onEnd() {}
    };

    void onStart();

    void onProgress(long bytesTransferred, long contentLength);

    void onEnd();
}
