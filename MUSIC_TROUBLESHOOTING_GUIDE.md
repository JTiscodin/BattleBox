# 🎵 BattleBox Music Troubleshooting Guide

## Quick Test Commands

### 1. Test Your Resource Pack Installation
```
/playsound battlebox.music master @p
```
**Expected Result:** Music should play if resource pack is properly installed.

### 2. Test BattleBox Plugin Music System
```
/battlebox testmusic
```
**Expected Result:** Should play the same music through the plugin's system.

### 3. Test Teleportation Music
```
/battlebox join <worldname>
```
**Expected Result:** Music should play automatically 1 second after teleportation.

---

## ✅ Resource Pack Checklist

### File Structure (Must be exact):
```
your-battlebox-pack.zip
├── pack.mcmeta
└── assets/
    └── minecraft/
        ├── sounds.json
        └── sounds/
            └── battlebox/
                └── battlebox.ogg
```

### sounds.json Content (Must match exactly):
```json
{
  "battlebox.music": {
    "sounds": [
      {
        "name": "battlebox/battlebox",
        "stream": true
      }
    ]
  }
}
```

### pack.mcmeta Content:
```json
{
  "pack": {
    "pack_format": 15,
    "description": "BattleBox Custom Music Pack"
  }
}
```

---

## 🔍 Common Issues & Solutions

### Issue 1: "No music plays at all"
**Causes:**
- Resource pack not installed
- Wrong file format (must be .ogg, not .mp3)
- Incorrect sounds.json syntax

**Solutions:**
1. Test with `/playsound battlebox.music master @p`
2. Check resource pack is enabled in Minecraft settings
3. Verify .ogg file is in correct location
4. Check sounds.json syntax with JSON validator

### Issue 2: "Test command works but teleport doesn't"
**Causes:**
- Plugin not calling music method
- Teleportation not completing properly

**Solutions:**
1. Check server console for "Playing BattleBox music for [playername]"
2. Ensure teleportation completes successfully
3. Check if player has required permissions

### Issue 3: "Music plays but stops immediately"
**Causes:**
- File corruption
- Wrong audio format
- Client audio settings

**Solutions:**
1. Re-encode audio file to .ogg format
2. Check player's music volume is turned up
3. Test with a shorter audio file first

---

## 📊 Server Console Debug Info

### What to Look For:
```
[INFO] Playing BattleBox music for PlayerName at world(x,y,z)
[INFO] Test music command used by PlayerName
[INFO] Resource pack confirmed for PlayerName
```

### Error Messages to Check:
```
[WARNING] Failed to play BattleBox music for PlayerName: [error]
[WARNING] Resource pack issue for PlayerName
```

---

## 🧪 Step-by-Step Testing

### Step 1: Verify Resource Pack
1. Install resource pack in client
2. Join server
3. Run `/playsound battlebox.music master @p`
4. ✅ Music should play

### Step 2: Test Plugin System
1. Run `/battlebox testmusic`
2. Check console for log message
3. ✅ Music should play through plugin

### Step 3: Test Teleportation
1. Run `/battlebox join <world>` or `/battlebox tp <world>`
2. Wait for teleportation to complete
3. Wait 1 second for scheduler delay
4. ✅ Music should play automatically

### Step 4: Verify Logs
1. Check server console for music playback messages
2. Look for any error messages
3. ✅ Should see "Playing BattleBox music for [player]"

---

## 🔧 Configuration Values

### Current Plugin Settings:
- **Sound Event:** `"battlebox.music"`
- **Audio File:** `battlebox/battlebox.ogg`
- **Volume:** `1.0f` (100%)
- **Pitch:** `1.0f` (normal)
- **Category:** `SoundCategory.MUSIC`
- **Delay:** `20 ticks` (1 second)

### To Modify Volume:
In `BattleBox.java`, change the volume value:
```java
player.playSound(
    teleportLocation,
    BATTLEBOX_MUSIC_SOUND,
    SoundCategory.MUSIC,
    0.8f,  // Change this (0.0 to 1.0)
    1.0f
);
```

### To Modify Delay:
In `BattleBox.java`, change the scheduler delay:
```java
}, 40L); // Change from 20L to 40L for 2 seconds
```

---

## 📞 If Music Still Doesn't Work

### Double-Check These Requirements:
1. ✅ Minecraft client has resource pack enabled
2. ✅ Audio file is `.ogg` format (not `.mp3`)
3. ✅ File path is exact: `assets/minecraft/sounds/battlebox/battlebox.ogg`
4. ✅ sounds.json references `"battlebox/battlebox"` (no .ogg extension)
5. ✅ Sound event name is `"battlebox.music"`
6. ✅ Player's music volume is turned up in settings

### Alternative Test:
Try using a vanilla Minecraft sound first to test the system:
```java
player.playSound(location, Sound.MUSIC_DISC_CAT, SoundCategory.MUSIC, 1.0f, 1.0f);
```

If vanilla sounds work but custom sounds don't, the issue is with the resource pack setup.

---

*This guide should help identify and resolve any music playback issues!* 🎵✨
