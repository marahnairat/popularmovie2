package com.example.popularmovies2;

import android.content.Context;
import android.content.Intent;
import com.example.popularmovies2.model.Movie;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.*;


public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    private static Movie[] mMovies;
    private Context mContext;

    public ImageAdapter(Context mContext, Movie[] mMovies) {
        this.mMovies = mMovies;

        if (mContext == null) {
            try{
                Thread.sleep(1000);
            } catch(InterruptedException exception){
                exception.printStackTrace();
            }
        } else {
            this.mContext = mContext;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView mImageView;

        public ViewHolder(ImageView v) {
            super(v);
            mImageView = v;
        }
    }
    @NonNull
    @Override
    // Create new views (Invoked by the Layout Manager)
    public ImageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Create a new view
        ImageView v = (ImageView) LayoutInflater.from(parent.getContext ())
                .inflate (R.layout.image_thumb_view, parent, false);

        ViewHolder vh = new ViewHolder (v);
        return vh;
    }

    @Override
    // Replace the contents of a view (Invoked by the layout manager)
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        Picasso picasso = Picasso.get();
        //picasso.setLoggingEnabled(true);
        picasso.load(mMovies[position].getPosterPath())
                .fit()
                .error(R.mipmap.ic_launcher)
                .placeholder(R.mipmap.ic_launcher_round)
                .into((ImageView) holder.mImageView.findViewById (R.id.image_view));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, MovieDetails.class);
                intent.putExtra("movie", mMovies[position]);
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (mMovies == null || mMovies.length == 0) {
            return -1;
        }
        return mMovies.length;
    }

    public void setMovies(Movie [] movies) {
        mMovies = movies;
    }

}