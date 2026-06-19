<template>
  <div class="w-full max-w-2xl mx-auto space-y-6">
    <div class="bg-gray-900 rounded-xl p-6 border border-gray-700">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-xl font-bold text-green-400">游戏大厅</h3>
        <div class="flex items-center gap-2">
          <input
            v-model="editingName"
            @blur="updateName"
            @keyup.enter="updateName"
            class="bg-gray-800 border border-gray-600 rounded px-3 py-1 text-sm text-white focus:outline-none focus:border-green-500"
            placeholder="输入昵称"
          />
        </div>
      </div>

      <div class="flex gap-3 mb-6">
        <button
          @click="showCreateModal = true"
          class="flex-1 py-3 bg-green-600 hover:bg-green-500 text-white rounded-lg transition-colors font-medium"
        >
          + 创建房间
        </button>
        <button
          @click="store.switchToSoloMode()"
          class="flex-1 py-3 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition-colors font-medium"
        >
          单人对弈模式
        </button>
      </div>

      <div v-if="store.roomError" class="mb-4 p-3 bg-red-900/30 border border-red-600/50 rounded-lg text-red-400 text-sm">
        {{ store.roomError }}
      </div>

      <h4 class="text-lg font-semibold text-gray-300 mb-3">房间列表</h4>
      <div v-if="store.rooms.length === 0" class="text-center py-8 text-gray-500">
        暂无房间，点击上方按钮创建房间
      </div>
      <div v-else class="space-y-3">
        <div
          v-for="room in store.rooms"
          :key="room.id"
          class="bg-gray-800 rounded-lg p-4 border border-gray-700 hover:border-gray-600 transition-colors"
        >
          <div class="flex items-center justify-between">
            <div>
              <div class="font-medium text-white">{{ room.name }}</div>
              <div class="text-sm text-gray-400 mt-1">
                {{ room.players.length }} / {{ room.maxPlayers }} 人 ·
                <span :class="statusColor(room.status)">{{ statusText(room.status) }}</span>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <div class="flex -space-x-2">
                <div
                  v-for="player in room.players"
                  :key="player.id"
                  class="w-8 h-8 rounded-full bg-gray-600 border-2 border-gray-800 flex items-center justify-center text-xs text-white"
                  :title="player.name"
                >
                  {{ player.name.charAt(0) }}
                </div>
              </div>
              <button
                @click="handleJoin(room.id)"
                :disabled="room.players.length >= room.maxPlayers || room.status === 'PLAYING'"
                class="px-4 py-2 bg-green-600 hover:bg-green-500 disabled:bg-gray-600 disabled:cursor-not-allowed text-white rounded-lg transition-colors text-sm"
              >
                加入
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Create Room Modal -->
    <div
      v-if="showCreateModal"
      class="fixed inset-0 bg-black/70 flex items-center justify-center z-50"
      @click.self="showCreateModal = false"
    >
      <div class="bg-gray-900 rounded-xl p-6 border border-gray-700 w-full max-w-md mx-4">
        <h4 class="text-xl font-bold text-green-400 mb-4">创建房间</h4>
        <div class="mb-4">
          <label class="block text-sm text-gray-400 mb-2">房间名称</label>
          <input
            v-model="newRoomName"
            class="w-full bg-gray-800 border border-gray-600 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-green-500"
            placeholder="输入房间名称"
          />
        </div>
        <div class="flex gap-3">
          <button
            @click="showCreateModal = false"
            class="flex-1 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition-colors"
          >
            取消
          </button>
          <button
            @click="handleCreate"
            class="flex-1 py-2 bg-green-600 hover:bg-green-500 text-white rounded-lg transition-colors"
          >
            创建
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useGameStore } from '../store/game';
import type { RoomStatus } from '../types';

const store = useGameStore();

const showCreateModal = ref(false);
const newRoomName = ref('');
const editingName = ref(store.currentUser.name);

let pollTimer: ReturnType<typeof setInterval> | null = null;

onMounted(() => {
  store.fetchRooms();
  pollTimer = setInterval(() => {
    store.fetchRooms();
  }, 3000);
});

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer);
  }
});

function updateName() {
  if (editingName.value.trim()) {
    store.setUserName(editingName.value.trim());
  } else {
    editingName.value = store.currentUser.name;
  }
}

async function handleCreate() {
  const name = newRoomName.value.trim() || `${store.currentUser.name}的房间`;
  const success = await store.createRoom(name);
  if (success) {
    showCreateModal.value = false;
    newRoomName.value = '';
  }
}

async function handleJoin(roomId: string) {
  await store.joinRoom(roomId);
}

function statusText(status: RoomStatus): string {
  switch (status) {
    case 'WAITING': return '等待加入';
    case 'PREPARING': return '准备中';
    case 'PLAYING': return '游戏中';
    case 'FINISHED': return '已结束';
    default: return '';
  }
}

function statusColor(status: RoomStatus): string {
  switch (status) {
    case 'WAITING': return 'text-yellow-400';
    case 'PREPARING': return 'text-blue-400';
    case 'PLAYING': return 'text-green-400';
    case 'FINISHED': return 'text-gray-500';
    default: return 'text-gray-400';
  }
}
</script>
