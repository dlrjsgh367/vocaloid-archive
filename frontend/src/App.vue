<template>
  <div class="app">
    <header class="app-header">
      <RouterLink to="/">VocaloidArchive</RouterLink>
      <nav>
        <RouterLink to="/search">Search</RouterLink>
        <RouterLink to="/playlists">Playlists</RouterLink>
        <RouterLink to="/login">Login</RouterLink>
      </nav>
    </header>
    <main>
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterLink, RouterView } from 'vue-router';
</script>

<style scoped>
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  border-bottom: 1px solid #eee;
}
.app-header nav a { margin-left: 16px; }
</style>
