package com.example.popularmovies2;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.popularmovies2.database.AppDatabase;
import com.example.popularmovies2.model.Movie;
import com.example.popularmovies2.utils.Constants;
import com.example.popularmovies2.utils.JsonUtils;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MovieDetails extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private ReviewAdapter mReviewAdapter;
    private TextView reviewLabel;
    private View divider;
    private Movie movie;
    private ToggleButton favoriteBtn;
    private AppDatabase mDb;
    private String releaseDate;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_movie_details);

       androidx.appcompat.app.ActionBar actionBar;
        actionBar = this.getSupportActionBar ();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled (true);
        }

        Intent intent = getIntent();
        if (intent == null) { closeOnError(); }

        movie = intent.getParcelableExtra("movie");
        mDb = AppDatabase.getInstance (getApplicationContext ());

        setupDetailsUI (movie);

        setUpFavoriteMovieButton ();

        // When toggle is changed on/off
        favoriteBtn.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                // Toggle is Enabled
                favoriteBtn.getTextOn ();
                onFavoriteButtonClicked ();
            } else {
                // Toggle is disabled
                favoriteBtn.setTextColor (Color.parseColor("#000000"));
                favoriteBtn.getTextOff();

                AppExecutor.getInstance ().diskIO ().execute (() -> runOnUiThread(() ->
                        mDb.movieDao ().deleteMovie (movie.getMovieId ())));
            }
        });


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState (outState);
    }

    protected void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState (savedState);
    }

    public boolean onOptionsSelectedItem(MenuItem item) {
        int id = item.getItemId ();
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask (this);
        }
        return super.onOptionsItemSelected (item);
    }

    void closeOnError() {
        finish();
        Toast.makeText(this, "Something went wrong.", Toast.LENGTH_SHORT).show();
    }

    private void setupDetailsUI(Movie movie) {
        TextView originalTitleTV = findViewById (R.id.titleTextView);
        TextView ratingTV = findViewById (R.id.ratingTextView);
        TextView releaseDateTV = findViewById (R.id.releaseDateTextView);
        TextView overviewTV = findViewById (R.id.overviewTextView);
        ImageView posterIV = findViewById (R.id.posterImageView);
        Button trailerBtn = findViewById (R.id.watchTrailerBtn);
        favoriteBtn = findViewById (R.id.favoritesBtn);

        RecyclerView.LayoutManager mLayoutManager;

        mRecyclerView = (RecyclerView) findViewById(R.id.reviewsRecyclerView);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // TITLE
        originalTitleTV.setText(movie.getOriginalTitle());

        // VOTER AVERAGE / RATING
        ratingTV.setText (String.valueOf(movie.getVoterAverage ()) + Constants.OUT_OF_RATING_STRING);

        // IMAGE
        Picasso.get()
                .load(movie.getPosterPath())
                .fit ().centerCrop ()
                .error(R.mipmap.ic_launcher_round)
                .placeholder(R.mipmap.ic_launcher_round)
                .into(posterIV);

        // OVERVIEW
        overviewTV.setText (movie.getOverview ());

        // RELEASE DATE
        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Constants.UNFORMATED_DATE_STRING);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat DATE_FORMAT = new SimpleDateFormat (Constants.FULL_DATE_FORMAT_STRING);

        try {
            Date date = simpleDateFormat.parse(movie.getReleaseDate ());
            releaseDate = DATE_FORMAT.format(date);
        } catch (ParseException e) {
            e.printStackTrace ();
        }

        releaseDateTV.setText (releaseDate);

        // TRAILER BUTTON
        new TrailerButtonAsyncTask (trailerBtn).execute(String.valueOf(movie.getMovieId ()), Constants.VIDEO_QUERY_PARAM);

        // LOAD REVIEWS
        new ReviewsAsyncTask ().execute(String.valueOf(movie.getMovieId ()), Constants.REVIEW_URL_QUERY_PARAM);

        // INITIAL BUTTON VALUES
        favoriteBtn.setTextOn(Constants.FAVORITED_STRING);
        favoriteBtn.setTextOff(Constants.ADD_TO_FAVORITES_STRING);
    }

    /*** ASYNC TASK FOR THE "WATCH TRAILER" BUTTON ***/
    private class TrailerButtonAsyncTask extends AsyncTask<String, Void, String> {
        private final Button button;
        String trailerKey = null;

        public TrailerButtonAsyncTask(Button button) {
            this.button = button;
        }

        @Override
        protected String doInBackground(String... strings) {
            Movie[] movies;
            try {
                URL url = JsonUtils.buildMovieIdUrl(strings[0], strings[1]);
                String movieSearchResults = JsonUtils.getResponseFromHttpUrl(url);

                JSONObject root = new JSONObject(movieSearchResults);
                JSONArray resultsArray = root.getJSONArray (Constants.RESULTS_QUERY_PARAM);

                if (resultsArray.length () == 0) {
                    trailerKey = null;
                } else {
                    movies = new Movie[resultsArray.length ()];
                    for (int i = 0; i < resultsArray.length(); i++) {
                        // Initialize each object before it can be used
                        movies[i] = new Movie();

                        // Object contains all tags we're looking for
                        JSONObject movieInfo = resultsArray.getJSONObject(i);

                        // Store data in movie object
                        movies[i].setTrailerPath(movieInfo.getString(Constants.VIDEO_TRAILER_KEY_PARAM));
                    }
                    // Returns only the first trailer from the results array, since there can be multiple trailers
                    return movies[0].getTrailerPath();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace ();
            } catch (IOException e) {
                e.printStackTrace ();
            } catch (JSONException e) {
                e.printStackTrace ();
            }
            return trailerKey;
        }

        @SuppressLint("WrongConstant")
        protected void onPostExecute(String temp) {
            button.setOnClickListener((View v) -> {
                if (temp == null) {
                    Toast.makeText(getApplicationContext(), Constants.NO_TRAILERS, Constants.TOAST_DURATION).show();
                } else {
                    watchYoutubeVideo (getApplicationContext (), temp);
                }
            });

        }
    }

    /*** YT VIDEO INTENT. https://stackoverflow.com/questions/574195/android-youtube-app-play-video-intent ***/
    public static void watchYoutubeVideo(Context context, String id){
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.YOUTUBE_APP_BASE + id));
        Intent webIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(Constants.YOUTUBE_BASE_URL + id));
        // If youtube is not installed, plays from web
        try {
            context.startActivity(appIntent);
        } catch (ActivityNotFoundException ex) {
            context.startActivity(webIntent);
        }
    }

    /*** ASYNC TASK TO FETCH REVIEWS ***/
    private class ReviewsAsyncTask extends AsyncTask<String, Void, Movie[]> {
        @Override
        protected Movie[] doInBackground(String... strings) {
            try {
                URL url = JsonUtils.buildMovieIdUrl(strings[0], strings[1]);
                String movieSearchResults = JsonUtils.getResponseFromHttpUrl(url);
                return setMovieDataToArray(movieSearchResults);
            } catch (IOException | JSONException e) {
                e.printStackTrace ();
            }
            return null;
        }
        protected void onPostExecute(Movie[] movies) {
            // specify an adapter
            mReviewAdapter = new ReviewAdapter(movies, getApplicationContext ());

            if(mReviewAdapter.getItemCount () == -1) {
                // If there's no reviews, make the label and divider for the reviews visibility to none
                reviewLabel = findViewById (R.id.textView);
                divider = findViewById (R.id.divider2);
                reviewLabel.setVisibility (TextView.GONE);
                divider.setVisibility (View.GONE);
            } else {
                mRecyclerView.setAdapter(mReviewAdapter);
                mRecyclerView.setNestedScrollingEnabled (false);
            }

        }
    }

    /*** ADD REVIEW DATA TO MOVIE OBJECT ***/
    public Movie[] setMovieDataToArray(String jsonResults) throws JSONException {
        JSONObject root = new JSONObject(jsonResults);
        JSONArray resultsArray = root.getJSONArray (Constants.RESULTS_QUERY_PARAM);
        Movie[] movies = new Movie[resultsArray.length ()];

        for (int i = 0; i < resultsArray.length(); i++) {
            // Initialize each object before it can be used
            movies[i] = new Movie();

            // Object contains all tags we're looking for
            JSONObject movieInfo = resultsArray.getJSONObject(i);

            // Store data in movie object
            movies[i].setReviewAuthor (movieInfo.getString(Constants.REVIEW_AUTHOR_QUERY_PARAM));
            movies[i].setReviewContents (movieInfo.getString(Constants.REVIEW_QUERY_PARAM));
            movies[i].setReviewUrl (movieInfo.getString (Constants.REVIEW_URL_PARAM));
        }
        return movies;
    }

    /*** FAVORITE MOVIE BUTTON IS CALLED WHEN "ADD TO FAVORITES" BUTTON IS CLICKED***/
    public void onFavoriteButtonClicked() {
        final Movie movie = getIntent().getExtras().getParcelable ( "movie");
        AppExecutor.getInstance ().diskIO ().execute (() -> mDb.movieDao ().insertMovie (movie));
    }

    private void setUpFavoriteMovieButton () {
        MovieDetailsViewModelFactory factory =
                new MovieDetailsViewModelFactory (mDb, movie.getMovieId ());
        final MovieDetailsViewModel viewModel =
                ViewModelProviders.of(this, factory).get(MovieDetailsViewModel.class);

        viewModel.getMovie ().observe (this, new Observer<Movie>() {
            @Override
            public void onChanged(@Nullable Movie movieInDb) {
                viewModel.getMovie ().removeObserver (this);

                if (movieInDb == null) {
                    favoriteBtn.setTextColor (Color.parseColor("#000000"));
                    favoriteBtn.setChecked (false);
                    favoriteBtn.getTextOff();
                } else if ((movie.getMovieId () == movieInDb.getMovieId ()) && !favoriteBtn.isChecked ()){
                    favoriteBtn.setChecked (true);
                    favoriteBtn.setText(Constants.FAVORITED_STRING);
                    favoriteBtn.setTextColor (Color.parseColor("#b5001e"));
                } else {
                    favoriteBtn.setTextColor (Color.parseColor("#000000"));
                    favoriteBtn.setChecked (false);
                    favoriteBtn.getTextOff();
                }
            }
        });
    }
}