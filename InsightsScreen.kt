// Changes made to fix unresolved references

// Old iteration: d.topTracks.forEachIndexed { i, t ->
// New iteration: d.topTracks.forEachIndexed { i, track ->

d.topTracks.forEachIndexed { i, track ->
    // Updated references from `t.title` to `track.title`
    val title = track.title
    // Updated references from `t.durationSec` to `track.durationSec`
    val duration = track.durationSec

    // Additional logic goes here...
}