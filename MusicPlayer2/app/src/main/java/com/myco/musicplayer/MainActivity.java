package com.myco.musicplayer;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private FloatingActionButton btnPlayPause;
    private ImageButton btnNext, btnPrevious, btnShuffle, btnRepeat, btnMenu;
    private SeekBar seekBar;
    private TextView tvSongName, tvArtist, tvCurrentTime, tvTotalTime;
    private ImageView ivAlbumArt;
    private DrawerLayout drawerLayout;
    private RecyclerView rvSongs;
    private EditText etSearch;
    private VisualizerView visualizer;

    private Handler handler = new Handler();
    private List<Song> songList = new ArrayList<>();
    private List<Song> filteredList = new ArrayList<>();
    private SongAdapter adapter;
    private int currentSongIndex = -1;
    private boolean isShuffle = false;
    private boolean isRepeat = false;

    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupRecyclerView();
        checkPermissions();

        // Initial GIF display
        loadGif(false);

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        btnPlayPause.setOnClickListener(v -> {
            if (mediaPlayer == null) {
                if (!songList.isEmpty()) playNewSong(0);
                return;
            }
            if (mediaPlayer.isPlaying()) pauseMusic();
            else playMusic();
        });

        btnNext.setOnClickListener(v -> playNextSong());
        btnPrevious.setOnClickListener(v -> playPreviousSong());

        btnShuffle.setOnClickListener(v -> {
            isShuffle = !isShuffle;
            btnShuffle.setColorFilter(isShuffle ? 
                    ContextCompat.getColor(this, R.color.primary_green) : 
                    ContextCompat.getColor(this, R.color.text_secondary));
            Toast.makeText(this, isShuffle ? "Shuffle On" : "Shuffle Off", Toast.LENGTH_SHORT).show();
        });

        btnRepeat.setOnClickListener(v -> {
            isRepeat = !isRepeat;
            btnRepeat.setColorFilter(isRepeat ? 
                    ContextCompat.getColor(this, R.color.primary_green) : 
                    ContextCompat.getColor(this, R.color.text_secondary));
            Toast.makeText(this, isRepeat ? "Repeat On" : "Repeat Off", Toast.LENGTH_SHORT).show();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) mediaPlayer.seekTo(progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadGif(boolean isPlaying) {
        int gifRes = getResources().getIdentifier("music_gif", "drawable", getPackageName());
        
        if (gifRes == 0) {
            gifRes = getResources().getIdentifier("music_gif", "raw", getPackageName());
        }

        if (isPlaying && gifRes != 0) {
            Glide.with(this).asGif().load(gifRes).into(ivAlbumArt);
        } else if (gifRes != 0) {
            Glide.with(this).asBitmap().load(gifRes).into(ivAlbumArt);
        } else {
            ivAlbumArt.setImageResource(android.R.drawable.ic_media_play);
            ivAlbumArt.setColorFilter(ContextCompat.getColor(this, R.color.primary_green));
        }
    }

    private void filter(String text) {
        filteredList.clear();
        for (Song song : songList) {
            if (song.getTitle().toLowerCase().contains(text.toLowerCase()) || 
                song.getArtist().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(song);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void initViews() {
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnRepeat = findViewById(R.id.btnRepeat);
        btnMenu = findViewById(R.id.btnMenu);
        seekBar = findViewById(R.id.seekBar);
        tvSongName = findViewById(R.id.tvSongName);
        tvArtist = findViewById(R.id.tvArtist);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        ivAlbumArt = findViewById(R.id.ivAlbumArt);
        drawerLayout = findViewById(R.id.drawerLayout);
        rvSongs = findViewById(R.id.rvSongs);
        etSearch = findViewById(R.id.etSearch);
        visualizer = findViewById(R.id.visualizer);
    }

    private void setupRecyclerView() {
        try {
            int resId = getResources().getIdentifier("naat", "raw", getPackageName());
            if (resId != 0) {
                songList.add(new Song("Naat Sharif", "Official", resId));
            }
        } catch (Exception ignored) {}

        filteredList.addAll(songList);
        rvSongs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(filteredList, song -> {
            int index = songList.indexOf(song);
            playNewSong(index);
            drawerLayout.closeDrawer(GravityCompat.START);
        });
        rvSongs.setAdapter(adapter);
    }

    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            loadSongsFromStorage();
        }
    }

    private void loadSongsFromStorage() {
        ContentResolver contentResolver = getContentResolver();
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        Cursor songCursor = contentResolver.query(songUri, null, selection, null, null);

        if (songCursor != null && songCursor.moveToFirst()) {
            int titleCol = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistCol = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int idCol = songCursor.getColumnIndex(MediaStore.Audio.Media._ID);

            do {
                String title = songCursor.getString(titleCol);
                String artist = songCursor.getString(artistCol);
                long id = songCursor.getLong(idCol);
                Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                songList.add(new Song(title, artist, uri));
            } while (songCursor.moveToNext());
            songCursor.close();
        }
        filteredList.clear();
        filteredList.addAll(songList);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            loadSongsFromStorage();
        }
    }

    private void playNewSong(int index) {
        if (index < 0 || index >= songList.size()) return;
        currentSongIndex = index;
        Song song = songList.get(index);

        if (mediaPlayer != null) {
            if (visualizer != null) visualizer.release();
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        try {
            if (song.isFromRes()) {
                mediaPlayer = MediaPlayer.create(this, song.getResId());
            } else {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(this, song.getUri());
                mediaPlayer.prepare();
            }

            if (mediaPlayer != null) {
                tvSongName.setText(song.getTitle());
                tvArtist.setText(song.getArtist());
                tvTotalTime.setText(formatTime(mediaPlayer.getDuration()));
                seekBar.setMax(mediaPlayer.getDuration());

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    visualizer.setAudioSessionId(mediaPlayer.getAudioSessionId());
                }

                mediaPlayer.setOnCompletionListener(mp -> {
                    if (isRepeat) playNewSong(currentSongIndex);
                    else playNextSong();
                });

                playMusic();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Playback Error", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void playNextSong() {
        if (songList.isEmpty()) return;
        int nextIndex = isShuffle ? new Random().nextInt(songList.size()) : (currentSongIndex + 1) % songList.size();
        playNewSong(nextIndex);
    }

    private void playPreviousSong() {
        if (songList.isEmpty()) return;
        int prevIndex = (currentSongIndex - 1 + songList.size()) % songList.size();
        playNewSong(prevIndex);
    }

    private void playMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            loadGif(true);
            updateSeekBar();
        }
    }

    private void pauseMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            loadGif(false);
        }
    }

    private void updateSeekBar() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            int currentPos = mediaPlayer.getCurrentPosition();
            seekBar.setProgress(currentPos);
            tvCurrentTime.setText(formatTime(currentPos));
            handler.postDelayed(this::updateSeekBar, 1000);
        }
    }

    private String formatTime(int ms) {
        int m = (ms / 1000) / 60;
        int s = (ms / 1000) % 60;
        return String.format("%d:%02d", m, s);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (visualizer != null) visualizer.release();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (visualizer != null) visualizer.release();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public static class Song {
        private String title, artist;
        private Uri uri;
        private int resId;
        private boolean isFromRes;

        public Song(String title, String artist, int resId) {
            this.title = title; this.artist = artist; this.resId = resId; this.isFromRes = true;
        }
        public Song(String title, String artist, Uri uri) {
            this.title = title; this.artist = artist; this.uri = uri; this.isFromRes = false;
        }
        public String getTitle() { return title; }
        public String getArtist() { return artist; }
        public Uri getUri() { return uri; }
        public int getResId() { return resId; }
        public boolean isFromRes() { return isFromRes; }
    }

    public static class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {
        private List<Song> songs;
        private OnItemClickListener listener;
        public interface OnItemClickListener { void onItemClick(Song song); }
        public SongAdapter(List<Song> songs, OnItemClickListener listener) { this.songs = songs; this.listener = listener; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Song s = songs.get(position);
            holder.t1.setText(s.getTitle());
            holder.t2.setText(s.getArtist());
            holder.t1.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white));
            holder.t2.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.secondary_text_dark));
            holder.itemView.setOnClickListener(v -> listener.onItemClick(s));
        }

        @Override
        public int getItemCount() { return songs.size(); }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView t1, t2;
            public ViewHolder(View v) { super(v); t1 = v.findViewById(android.R.id.text1); t2 = v.findViewById(android.R.id.text2); }
        }
    }
}
