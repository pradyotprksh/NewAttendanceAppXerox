package pradyotprakash.application.com.newattendanceappxerox;

import android.support.annotation.NonNull;

public class NotesId {
    public String noteId;

    public <T extends NotesId> T withId(@NonNull final String id) {
        this.noteId = id;
        return (T) this;
    }
}
