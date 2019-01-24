package ru.zont.mvc;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

@SuppressWarnings({"unchecked", "CanBeFinal"})
public class ArtifactObject implements Parcelable {
    abstract class STATUS {
        static final int ERROR = -99;
        static final int ERROR_LEARN = -1;
        static final int READY_TL = 0;
        static final int DOWNLOADING = 1;
        static final int TRAINING = 2;
        static final int READY_FU = 3;
        static final int OUTDATED = 4;
    }

    abstract class ACTION {
        static final int CREATED = 0;
        static final int EDITED = 1;
        static final int STARTED_TRAINING = 2;
        static final int TRAINED = 3;
    }

    private String id = new RandomString(16, new Random()).nextString();

    private String title;
    private ArrayList<Query> queries;
    private String thumbnail;
    private int status;
    private int lastActType;
    private long lastAct;
    private long created;
    private long learned;
    private boolean enabled;

    //private ArrayList<File> customImages;

    ArtifactObject(String title, ArrayList<Query> queries/*, ArrayList<File> customImages*/) {
        this.title = title;
        this.queries = queries;
        status = STATUS.READY_TL;
        lastActType = ACTION.CREATED;
        lastAct = System.currentTimeMillis();
        created = lastAct;
        learned = -1;
        enabled = true;
        //this.customImages = customImages;
    }

    int getQueriesSize() {
        return queries.size();
    }

    int getTotalBlacklisted() {
        int res = 0;
        for (Query q : queries)
            if (q.blacklist != null)
                res += q.blacklist.size();
        return res;
    }

    ArrayList<String> getBlacklist() {
        ArrayList<String> res = new ArrayList<>();
        for (Query q : queries)
            res.addAll(q.blacklist);
        return res;
    }

    String getId() { return id; }

    String getTitle() { return title; }

    ArrayList<Query> getQueries() { return queries; }

    void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }

    String getThumbnail() { return thumbnail; }

    int getStatus() { return status; }

    int getLastActType() { return lastActType; }

    long getLastAct() { return lastAct; }

    long getCreated() { return created; }

    long getLearned() { return learned; }

    int getTotal() {
        int res = 0;
        for (Query q : queries)
            res += q.whitelist.size();
        return res;
    }

    void setEnabled(boolean enabled) { this.enabled = enabled; }

    boolean isEnabled() { return enabled; }

    void edit(String title, ArrayList<Query> queries) {
        this.title = title;
        this.queries = queries;
        lastActType = ACTION.EDITED;
        lastAct = System.currentTimeMillis();
        if (learned >= 0) status = STATUS.OUTDATED;
    }

    public static class Query implements Parcelable {
        Query(String title) {
            this.title = title;
            blacklist = new ArrayList<>();
            whitelist = new ArrayList<>();
        }

        String title;
        ArrayList<String> blacklist;
        ArrayList<String> whitelist;

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Query)) return super.equals(obj);
            return Objects.equals(title, ((Query) obj).title)
                    && Objects.equals(blacklist, ((Query) obj).blacklist)
                    && Objects.equals(whitelist, ((Query) obj).whitelist);
        }

        public static final Parcelable.Creator<Query> CREATOR = new Creator<Query>() {
            @Override
            public Query createFromParcel(Parcel source) {
                return new Query(source);
            }

            @Override
            public Query[] newArray(int size) {
                return new Query[size];
            }
        };

        private Query(Parcel p) {
            title = p.readString();
            blacklist = p.readArrayList(String.class.getClassLoader());
            whitelist = p.readArrayList(String.class.getClassLoader());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(title);
            dest.writeList(blacklist);
            dest.writeList(whitelist);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return title;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ArtifactObject) return Objects.equals(((ArtifactObject) obj).id, id);
        else return super.equals(obj);
    }

    boolean dataEquals(ArtifactObject object) {
        return Objects.equals(id, object.id) &&
                Objects.equals(title, object.title) &&
                Objects.equals(queries, object.queries) &&
                Objects.equals(thumbnail, object.thumbnail) &&
                status == object.status &&
                lastActType == object.lastActType &&
                lastAct == object.lastAct &&
                created == object.created &&
                learned == object.learned &&
                enabled == object.enabled;
    }

    boolean dataEqualsExcId(ArtifactObject object) {
        return Objects.equals(title, object.title) &&
                Objects.equals(queries, object.queries) &&
                Objects.equals(thumbnail, object.thumbnail) &&
                status == object.status &&
                lastActType == object.lastActType &&
                lastAct == object.lastAct &&
                created == object.created &&
                learned == object.learned &&
                enabled == object.enabled;
    }

    public static final Parcelable.Creator<ArtifactObject> CREATOR = new Parcelable.Creator<ArtifactObject>() {
        @Override
        public ArtifactObject createFromParcel(Parcel source) {
            return new ArtifactObject(source);
        }

        @Override
        public ArtifactObject[] newArray(int size) {
            return new ArtifactObject[size];
        }
    };

    private ArtifactObject(Parcel parcel) {
        id = parcel.readString();
        title = parcel.readString();
        queries = parcel.readArrayList(Query.class.getClassLoader());
        thumbnail = parcel.readString();
        status = parcel.readInt();
        lastActType = parcel.readInt();
        lastAct = parcel.readLong();
        created = parcel.readLong();
        learned = parcel.readLong();
        enabled = parcel.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeList(queries);
        dest.writeString(thumbnail);
        dest.writeInt(status);
        dest.writeInt(lastActType);
        dest.writeLong(lastAct);
        dest.writeLong(created);
        dest.writeLong(learned);
        dest.writeByte((byte) (enabled ? 1 : 0));
    }
}
