package com.example.movesensemeasuresystem.activity_record;

import com.google.gson.annotations.SerializedName;

public class MovesenseGyroDataResponse {
    @SerializedName("Body")
    public final MovesenseGyroDataResponse.Body body;

    public MovesenseGyroDataResponse(MovesenseGyroDataResponse.Body body){
        this.body = body;
    }

    public static class Body{
        @SerializedName("Timestamp")
        public final long timestamp;

        @SerializedName("ArrayGyro")
        public final MovesenseGyroDataResponse.Array[] array;

        @SerializedName("Headers")
        public final MovesenseGyroDataResponse.Headers header;

        public Body(long timestamp, MovesenseGyroDataResponse.Array[] array, MovesenseGyroDataResponse.Headers header) {
            this.timestamp = timestamp;
            this.array = array;
            this.header = header;
        }
    }

    public static class Array {
        @SerializedName("x")
        public final float x;
        @SerializedName("y")

        public final float y;
        @SerializedName("z")
        public final float z;

        public Array(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static class Headers {
        @SerializedName("Param0")
        public final int param0;

        public Headers(int param0) {
            this.param0 = param0;
        }
    }
}
