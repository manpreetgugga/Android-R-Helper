package in.callific.androidr;

import android.net.Uri;

public interface IOnDocumentStreamCallBacks {
    void onSuccess(Uri uri);

    void onFail();
}
