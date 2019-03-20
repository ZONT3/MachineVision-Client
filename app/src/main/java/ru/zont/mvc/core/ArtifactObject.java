package ru.zont.mvc.core;

import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

@SuppressWarnings({"unchecked", "CanBeFinal"})
public class ArtifactObject implements Parcelable {
    public abstract class STATUS {

        public static final int ERROR = -99;
        public static final int ERROR_LEARN = -1;
        public static final int READY_TL = 0;
        public static final int DOWNLOADING = 1;
        public static final int TRAINING = 2;
        public static final int READY_FU = 3;
        public static final int OUTDATED = 4;
        public static final int NOT_MARKED = 5;
    }
    public abstract class ACTION {

        public static final int CREATED = 0;
        public static final int EDITED = 1;
        public static final int STARTED_TRAINING = 2;
        public static final int TRAINED = 3;
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
    public ArtifactObject(String title, ArrayList<Query> queries/*, ArrayList<File> customImages*/) {
        this.title = title;
        this.queries = queries;
        status = hasUnmarked() ? STATUS.NOT_MARKED : STATUS.READY_TL;
        lastActType = ACTION.CREATED;
        lastAct = System.currentTimeMillis();
        created = lastAct;
        learned = -1;
        enabled = true;
        thumbnail = queries.get(0).whitelist.get(0).link;
        //this.customImages = customImages;
    }

    public int getQueriesSize() {
        return queries.size();
    }

    public String getId() { return id; }

    public String getTitle() { return title; }

    public ArrayList<Query> getQueries() { return queries; }

    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }

    public String getThumbnail() { return thumbnail; }

    public int getStatus() { return status; }

    public int getLastActType() { return lastActType; }

    public long getLastAct() { return lastAct; }

    public long getCreated() { return created; }

    public long getLearned() { return learned; }

    public int getTotal() {
        int res = 0;
        for (Query q : queries)
            res += q.whitelist.size();
        return res;
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isEnabled() { return enabled; }

    public void edit(String title, ArrayList<Query> queries) {
        this.title = title;
        this.queries = queries;
        lastActType = ACTION.EDITED;
        lastAct = System.currentTimeMillis();
        if (learned >= 0) status = STATUS.OUTDATED;
        else if (hasUnmarked()) status = STATUS.NOT_MARKED;
        else status = STATUS.READY_TL;
    }

    public void setPseudo() {
        lastActType = ACTION.TRAINED;
        lastAct = (new Random().nextLong() % (System.currentTimeMillis() - 86400000L - 1552510800000L))
                + 1552510800000L;
        learned = lastAct;
        created = lastAct - 86400000L;
        status = STATUS.READY_FU;
    }

    public static class ImageItem implements Parcelable {

        public String link;
        public ArrayList<Integer[]> layout;
        public ImageItem(String link) {
            this.link = link;
            layout = new ArrayList<>();
        }

        public void addLayout(Rect selection) {
            addLayout(new Integer[] {
                    selection.left, selection.top,
                    selection.right, selection.bottom });
        }

        public void addLayout(Integer[] newLay) {
            if (newLay.length != 4) return;
            layout.add(newLay);
        }

        public Rect getRect(int i) {
            return new Rect(layout.get(i)[0], layout.get(i)[1],
                    layout.get(i)[2], layout.get(i)[3]);
        }

        private ImageItem(Parcel in) {
            link = in.readString();
            layout = in.readArrayList(Integer[].class.getClassLoader());
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof ImageItem
                    ? ((ImageItem) obj).link.equals(link)
                            && ((ImageItem) obj).layout.equals(layout)
                    : super.equals(obj);
        }

        public static final Creator<ImageItem> CREATOR = new Creator<ImageItem>() {
            @Override
            public ImageItem createFromParcel(Parcel in) {
                return new ImageItem(in);
            }

            @Override
            public ImageItem[] newArray(int size) {
                return new ImageItem[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(link);
            dest.writeList(layout);
        }

        @NonNull
        @Override
        public String toString() {
            return link != null ? link : super.toString();
        }

    }
    public static class Query implements Parcelable {

        public String title;
        public ArrayList<ImageItem> whitelist;
        public Query(String title) {
            this.title = title;
            whitelist = new ArrayList<>();
        }

        public void addNewImage(String link) {
            whitelist.add(new ImageItem(link));
        }

        public void addNewImages(String[] links) {
            for (String s : links)
                addNewImage(s);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Query)) return super.equals(obj);
            return Objects.equals(title, ((Query) obj).title)
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
            whitelist = p.readArrayList(ImageItem.class.getClassLoader());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(title);
            dest.writeList(whitelist);
        }
    }

    public boolean hasUnmarked() {
        for (Query q : queries)
            if (nextItem(q) != null)
                return true;
        return false;
    }

    @Nullable
    public static ImageItem nextItem(@NonNull Query query) {
        for (ImageItem i : query.whitelist)
            if (i.layout.size() == 0)
                return i;
        return null;
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

    public boolean queriesEquals(ArrayList<Query> queries) {
        for (Query q : queries)
            if (!this.queries.contains(q))
                return false;
        return true;
    }

    public boolean dataEquals(ArtifactObject object) {
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

    public boolean dataEquals(String title, ArrayList<Query> queries, String thumbnail) {
        return title.equals(this.title)
                && queries.equals(this.queries)
                && thumbnail.equals(this.thumbnail);
    }

    public boolean dataEqualsExcId(ArtifactObject object) {
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
