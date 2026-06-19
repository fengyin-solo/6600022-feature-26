<template>
  <div class="w-full max-w-2xl mx-auto space-y-6">
    <div class="bg-gray-900 rounded-xl p-6 border border-gray-700">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h3 class="text-xl font-bold text-green-400">{{ currentRoom?.name }}</h3>
          <div class="text-sm text-gray-400 mt-1">
            房间号: {{ roomIdShort }} ·
            <span :class="statusColor(currentRoom?.status)">{{ statusText(currentRoom?.status) }}</span>
          </div>
        </div>
        <div class="flex gap-2">
          <button
            @click="copyRoomId"
            class="px-3 py-2 bg-gray-700 hover:bg-gray-600 text-gray-300 rounded-lg transition-colors text-sm"
            :title="'复制房间号: ' + currentRoom?.id"
          >
            复制房间号
          </button>
          <button
            @click="handleLeave"
            class="px-3 py-2 bg-red-600/20 border border-red-600/50 text-red-400 rounded-lg hover:bg-red-600/30 transition-colors text-sm"
          >
            离开房间
          </button>
        </div>
      </div>

      <div v-if="store.roomError" class="mb-4 p-3 bg-red-900/30 border border-red-600/50 rounded-lg text-red-400 text-sm">
        {{ store.roomError }}
      </div>

      <h4 class="text-lg font-semibold text-gray-300 mb-4">玩家列表</h4>
      <div class="space-y-3 mb-6">
        <div
          v-for="(player, index) in currentRoom?.players"
          :key="player.id"
          class="bg-gray-800 rounded-lg p-4 border border-gray-700"
          :class="{ 'border-green-600/50 bg-green-900/10': player.isReady }"
        >
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <div
                class="w-10 h-10 rounded-full flex items-center justify-center text-white font-medium"
                :class="player.color === 1 ? 'bg-gray-700' : player.color === 2 ? 'bg-gray-200 text-gray-900' : 'bg-gray-600'"
              >
                {{ player.name.charAt(0) }}
              </div>
              <div>
                <div class="flex items-center gap-2">
                  <span class="font-medium text-white">{{ player.name }}</span>
                  <span
                    v-if="player.id === currentRoom?.ownerId"
                    class="text-xs px-2 py-0.5 bg-yellow-600/30 text-yellow-400 rounded-full"
                  >
                    房主
                  </span>
                  <span
                    v-if="player.color"
                    class="text-xs px-2 py-0.5 rounded-full"
                    :class="player.color === 1 ? 'bg-gray-700 text-gray-300' : 'bg-white text-black'"
                  >
                    {{ player.color === 1 ? '黑棋' : '白棋' }}
                  </span>
                </div>
                <div class="text-sm text-gray-400 mt-0.5">
                  {{ player.id === store.currentUser.id ? '你' : '玩家' + (index + 1) }}
                </div>
              </div>
            </div>
            <div class="flex items-center gap-2">
              <span
                v-if="player.isReady"
                class="text-xs px-3 py-1 bg-green-600/30 text-green-400 rounded-full font-medium"
              >
                已准备
              </span>
              <span
                v-else-if="currentRoom?.status === 'PREPARING' || currentRoom?.status === 'WAITING'"
                class="text-xs px-3 py-1 bg-gray-600/30 text-gray-400 rounded-full"
              >
                未准备
              </span>
            </div>
          </div>
        </div>

        <div
          v-if="currentRoom?.players.length === 1"
          class="bg-gray-800/50 rounded-lg p-4 border border-dashed border-gray-600 text-center text-gray-500"
        >
          等待另一位玩家加入...
        </div>
      </div>

      <div class="space-y-3">
        <template v-if="currentRoom?.status === 'WAITING' || currentRoom?.status === 'PREPARING'">
          <button
            v-if="!store.isRoomOwner"
            @click="toggleReady"
            class="w-full py-3 rounded-lg transition-colors font-medium text-lg"
            :class="isReady
              ? 'bg-gray-600 hover:bg-gray-500 text-white'
              : 'bg-green-600 hover:bg-green-500 text-white'"
          >
            {{ isReady ? '取消准备' : '准备' }}
          </button>

          <div v-else class="space-y-3">
            <div v-if="!store.allPlayersReady" class="text-center text-yellow-400 text-sm">
              {{ currentRoom?.players.length < 2 ? '等待玩家加入...' : '等待对方准备...' }}
            </div>
            <button
              @click="handleStart"
              :disabled="!store.canStartGame"
              class="w-full py-3 rounded-lg transition-colors font-medium text-lg"
              :class="store.canStartGame
                ? 'bg-green-600 hover:bg-green-500 text-white'
                : 'bg-gray-700 text-gray-500 cursor-not-allowed'"
            >
              开始游戏
            </button>
            <p class="text-xs text-gray-500 text-center">
              作为房主，你可以在所有玩家准备好后开始游戏
            </p>
          </div>
        </template>

        <div v-else-if="currentRoom?.status === 'PLAYING'" class="text-center">
          <div class="text-green-400 text-lg font-medium mb-2">游戏进行中</div>
          <div class="text-sm text-gray-400">
            轮到
            <span
              class="font-medium"
              :class="store.currentPlayer === 1 ? 'text-gray-300' : 'text-white'"
            >{{ store.currentPlayer === 1 ? '黑棋' : '白棋' }}</span>
            落子
          </div>
          <div v-if="store.myColor" class="text-sm text-gray-400 mt-1">
            你执
            <span
              class="font-medium"
              :class="store.myColor === 1 ? 'text-gray-300' : 'text-white'"
            >{{ store.myColor === 1 ? '黑棋' : '白棋' }}</span>
          </div>
        </div>

        <div v-else-if="currentRoom?.status === 'FINISHED'" class="text-center">
          <div class="text-yellow-400 text-lg font-medium">游戏已结束</div>
          <button
            @click="resetRoom"
            class="mt-3 px-6 py-2 bg-green-600 hover:bg-green-500 text-white rounded-lg transition-colors"
          >
            再来一局
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useGameStore } from '../store/game';
import type { RoomStatus } from '../types';

const store = useGameStore();

const currentRoom = computed(() => store.currentRoom);

const roomIdShort = computed(() => {
  const id = currentRoom.value?.id;
  if (!id) return '';
  return id.substring(0, 8) + '...';
});

const isReady = computed(() => store.currentPlayerInRoom?.isReady || false);

async function toggleReady() {
  await store.setReady(!isReady.value);
}

async function handleStart() {
  await store.startRoomGame();
}

async function handleLeave() {
  await store.leaveRoom();
}

function copyRoomId() {
  if (currentRoom.value?.id) {
    navigator.clipboard.writeText(currentRoom.value.id);
  }
}

function resetRoom() {
  store.setReady(false);
}

function statusText(status?: RoomStatus): string {
  switch (status) {
    case 'WAITING': return '等待加入';
    case 'PREPARING': return '准备中';
    case 'PLAYING': return '游戏中';
    case 'FINISHED': return '已结束';
    default: return '';
  }
}

function statusColor(status?: RoomStatus): string {
  switch (status) {
    case 'WAITING': return 'text-yellow-400';
    case 'PREPARING': return 'text-blue-400';
    case 'PLAYING': return 'text-green-400';
    case 'FINISHED': return 'text-gray-500';
    default: return 'text-gray-400';
  }
}
</script>
