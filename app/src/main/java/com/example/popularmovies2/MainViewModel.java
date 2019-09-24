package com.example.popularmovies2;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.popularmovies2.database.AppDatabase;
import com.example.popularmovies2.model.Movie;

public class MainViewModel extends AndroidViewModel {

    private LiveData<Movie[]> movies;

    public MainViewModel(@NonNull Application application) {
        super (application);
        AppDatabase database = AppDatabase.getInstance (this.getApplication ());
        movies = database.movieDao().loadAllMovies();
    }

    public LiveData<Movie[]> getMovies() {
        return movies;
    }
}

