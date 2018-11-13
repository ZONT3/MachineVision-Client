package ru.zont.mvc;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

class ArtifactObject implements Serializable {
    final String id = new RandomString(16, new Random()).nextString();;

    private String title;
    private ArrayList<Query> queries;
    private String thumbnail;
    //private ArrayList<File> customImages;

    ArtifactObject(String title, ArrayList<Query> queries/*, ArrayList<File> customImages*/) {
        this.title = title;
        this.queries = queries;
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

    String getTitle() { return title; }

    ArrayList<Query> getQueries() { return queries; }

    void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }

    public String getThumbnail() { return thumbnail; }

    static class Query implements Serializable {
        Query(String title) {
            this.title = title;
            blacklist = new ArrayList<>();
        }

        String title;
        ArrayList<String> blacklist;

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Query)) return super.equals(obj);
            return title.equals(((Query) obj).title) && blacklist.equals(((Query) obj).blacklist);
        }
    }

    @Override
    public String toString() {
        return title;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ArtifactObject) return ((ArtifactObject)obj).id.equals(id);
        else return super.equals(obj);
    }

    public boolean dataEquals(ArtifactObject object) {
        return id.equals(object.id) &&
                title.equals(object.title) &&
                queries.equals(object.queries) &&
                thumbnail.equals(object.thumbnail);
    }
}
