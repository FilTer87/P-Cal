<template>
  <div class="user-profile">
    <div class="bg-white dark:bg-gray-900 shadow-lg rounded-lg overflow-hidden">
      <!-- Profile Header -->
      <div class="px-6 py-8 bg-gradient-to-r from-blue-500 to-purple-600 relative">
        <!-- Back to Home Button -->
        <router-link
          to="/"
          class="absolute top-4 right-4 bg-white/20 backdrop-blur-sm hover:bg-white/30 text-white px-4 py-2 rounded-lg flex items-center space-x-2 transition-colors"
          title="Torna alla pagina principale"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
          </svg>
          <span class="text-sm">Home</span>
        </router-link>

        <div class="flex flex-col sm:flex-row items-center sm:items-start space-y-4 sm:space-y-0 sm:space-x-6">
          <!-- Avatar Section -->
          <div class="relative group">
            <div class="w-24 h-24 rounded-full bg-white shadow-lg flex items-center justify-center overflow-hidden">
              <img
                v-if="user?.avatar"
                :src="user.avatar"
                :alt="`${user.firstName || user.username}'s avatar`"
                class="w-full h-full object-cover"
              />
              <span
                v-else
                class="text-2xl font-bold text-gray-600"
              >
                {{ userInitials }}
              </span>
            </div>

            <!-- Avatar Upload Overlay - TODO: Implement backend avatar upload -->
            <!-- <div class="absolute inset-0 bg-black bg-opacity-50 rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer">
              <input
                ref="avatarInput"
                type="file"
                accept="image/*"
                @change="handleAvatarChange"
                class="hidden"
              />
              <button
                @click="triggerAvatarUpload"
                class="text-white hover:text-gray-200 transition-colors"
                title="Cambia avatar"
              >
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
              </button>
            </div> -->
          </div>

          <!-- User Info -->
          <div class="flex-1 text-center sm:text-left">
            <h1 class="text-2xl font-bold text-white mb-2">
              {{ userFullName || user?.username }}
            </h1>
            <p class="text-blue-100 mb-2">{{ user?.email }}</p>
            <p v-if="user?.createdAt" class="text-blue-200 text-sm">
              Membro dal {{ formatMemberSince(user.createdAt) }}
            </p>
          </div>
        </div>
      </div>

      <!-- Navigation Tabs (Desktop only) -->
      <div class="hidden md:block border-b border-gray-200 dark:border-gray-700">
        <nav class="-mb-px flex justify-center">
          <div class="flex space-x-1">
            <button
              v-for="tab in tabs"
              :key="tab.id"
              @click="activeTab = tab.id"
              :class="[
                'group inline-flex items-center py-4 px-6 border-b-2 font-medium text-sm transition-all duration-200 focus:outline-none focus:ring-0 whitespace-nowrap',
                activeTab === tab.id
                  ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                  : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300 hover:border-gray-300'
              ]"
            >
              <component :is="tab.icon" class="w-5 h-5 mr-2 flex-shrink-0" />
              <span>{{ tab.name }}</span>
            </button>
          </div>
        </nav>
      </div>

      <!-- Accordion Navigation (Mobile only) -->
      <div class="md:hidden">
        <div v-for="tab in tabs" :key="`mobile-${tab.id}`" class="border-b border-gray-200 dark:border-gray-700">
          <button
            @click="toggleMobileSection(tab.id)"
            class="w-full px-4 py-4 flex items-center justify-between hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors"
            :class="{ 'bg-blue-50/50 dark:bg-blue-900/10': activeTab === tab.id }"
          >
            <div class="flex items-center space-x-3">
              <component :is="tab.icon" class="w-5 h-5 flex-shrink-0" :class="activeTab === tab.id ? 'text-blue-600 dark:text-blue-400' : 'text-gray-500 dark:text-gray-400'" />
              <span class="font-medium text-sm" :class="activeTab === tab.id ? 'text-blue-600 dark:text-blue-400' : 'text-gray-900 dark:text-white'">
                {{ tab.name }}
              </span>
            </div>
            <svg
              class="w-5 h-5 transition-transform duration-200"
              :class="[
                activeTab === tab.id ? 'rotate-180 text-blue-600 dark:text-blue-400' : 'text-gray-400',
              ]"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
          </button>

          <!-- Mobile Tab Content -->
          <div v-show="activeTab === tab.id" class="py-2">
            <!-- Mobile: Personal Information -->
            <div v-if="tab.id === 'personal'" class="space-y-4">
              <div class="flex items-center justify-between">
                <h3 class="text-base font-semibold text-gray-900 dark:text-white">
                  Informazioni Personali
                </h3>
                <button
                  @click="toggleEditMode('personal')"
                  class="inline-flex items-center px-3 py-1.5 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-xs font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700"
                >
                  <svg v-if="!editModes.personal" class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                  </svg>
                  <svg v-else class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                  {{ editModes.personal ? 'Annulla' : 'Modifica' }}
                </button>
              </div>

              <form @submit.prevent="handlePersonalInfoSave" class="space-y-4">
                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Nome Utente</label>
                  <input v-model="profileForm.username" type="text" disabled
                    class="w-full px-3 py-2 border rounded-lg text-sm bg-gray-50 dark:bg-gray-800 text-gray-500 dark:text-gray-400 cursor-not-allowed" />
                  <!-- <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">Il nome utente non può essere modificato</p> -->
                </div>

                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Email</label>
                  <input v-model="profileForm.email" type="email"
                    :disabled="user?.emailVerified || !editModes.personal || isLoading"
                    :class="[
                      'w-full px-3 py-2 border rounded-lg text-sm',
                      user?.emailVerified || !editModes.personal
                        ? 'bg-gray-50 dark:bg-gray-800 text-gray-500 dark:text-gray-400 cursor-not-allowed'
                        : 'bg-white dark:bg-gray-800 border-gray-300 dark:border-gray-600'
                    ]" />
                  <p v-if="user?.emailVerified" class="mt-1 text-xs text-gray-500 dark:text-gray-400">
                    ✓ Email verificata e protetta
                  </p>
                  <p v-else class="mt-1 text-xs text-yellow-600 dark:text-yellow-400" v-if="editModes.personal">
                    ⚠ Email non verificata o verifica non richiesta
                  </p>
                  <p v-if="errors.email" class="mt-1 text-xs text-red-600">{{ errors.email }}</p>
                </div>

                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Nome</label>
                  <input :value="editModes.personal ? profileForm.firstName : (user?.firstName || '')"
                    @input="editModes.personal && (profileForm.firstName = ($event.target as HTMLInputElement).value)"
                    type="text" :disabled="!editModes.personal || isLoading"
                    :class="['w-full px-3 py-2 border rounded-lg text-sm', !editModes.personal ? 'bg-gray-50 dark:bg-gray-800 text-gray-500 dark:text-gray-400 cursor-not-allowed' : 'bg-white dark:bg-gray-800 border-gray-300 dark:border-gray-600']" />
                </div>

                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Cognome</label>
                  <input :value="editModes.personal ? profileForm.lastName : (user?.lastName || '')"
                    @input="editModes.personal && (profileForm.lastName = ($event.target as HTMLInputElement).value)"
                    type="text" :disabled="!editModes.personal || isLoading"
                    :class="['w-full px-3 py-2 border rounded-lg text-sm', !editModes.personal ? 'bg-gray-50 dark:bg-gray-800 text-gray-500 dark:text-gray-400 cursor-not-allowed' : 'bg-white dark:bg-gray-800 border-gray-300 dark:border-gray-600']" />
                </div>

                <button v-if="editModes.personal" type="submit" :disabled="isLoading"
                  class="w-full flex justify-center items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none disabled:opacity-50">
                  <LoadingSpinner v-if="isLoading" class="w-4 h-4 mr-2" />
                  {{ isLoading ? 'Salvando...' : 'Salva Modifiche' }}
                </button>
              </form>
            </div>

            <!-- Mobile: Security -->
            <div v-else-if="tab.id === 'security'" class="space-y-6">
              <!-- 2FA Status - same as desktop -->
              <div class="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <div class="flex items-center justify-between">
                  <div>
                    <h4 class="text-sm font-medium text-gray-900 dark:text-white mb-1">Autenticazione a Due Fattori (2FA)</h4>
                    <p class="text-xs text-gray-600 dark:text-gray-400">
                      {{ user?.twoFactorEnabled ? 'Abilitata' : 'Disabilitata' }}
                    </p>
                  </div>
                  <button v-if="!user?.twoFactorEnabled" @click="showTwoFactorSetupModal = true"
                    class="px-3 py-1.5 bg-green-600 text-white rounded-md hover:bg-green-700 text-xs">
                    Abilita 2FA
                  </button>
                  <button v-else @click="showTwoFactorDisableModal = true"
                    class="px-3 py-1.5 bg-red-600 text-white rounded-md hover:bg-red-700 text-xs">
                    Disabilita 2FA
                  </button>
                </div>
              </div>

              <!-- Password Change - Compact form for mobile -->
              <div>
                <div class="flex items-center justify-between mb-3">
                  <h4 class="text-sm font-medium text-gray-900 dark:text-white">Cambia Password</h4>
                  <button @click="toggleEditMode('password')"
                    class="px-3 py-1.5 border border-gray-300 dark:border-gray-600 rounded-md text-xs text-gray-700 dark:text-gray-300">
                    {{ editModes.password ? 'Annulla' : 'Modifica' }}
                  </button>
                </div>

                <form v-if="editModes.password" @submit.prevent="handleSecuritySave" class="space-y-3">
                  <div>
                    <label class="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">Password Attuale</label>
                    <input v-model="passwordForm.currentPassword" :type="showPasswords.current ? 'text' : 'password'"
                      class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-800" />
                  </div>

                  <div>
                    <label class="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">Nuova Password</label>
                    <input v-model="passwordForm.newPassword" :type="showPasswords.new ? 'text' : 'password'"
                      class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-800" />
                  </div>

                  <div>
                    <label class="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">Conferma Password</label>
                    <input v-model="passwordForm.confirmPassword" :type="showPasswords.confirm ? 'text' : 'password'"
                      class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-800" />
                  </div>

                  <button type="submit" :disabled="isLoading"
                    class="w-full px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 text-sm disabled:opacity-50">
                    {{ isLoading ? 'Salvando...' : 'Aggiorna Password' }}
                  </button>
                </form>
              </div>
            </div>

            <!-- Mobile: Preferences -->
            <div v-else-if="tab.id === 'preferences'" class="space-y-4">
              <h3 class="text-base font-semibold text-gray-900 dark:text-white mb-4">Preferenze</h3>

              <div class="space-y-4">
                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Tema</label>
                  <select v-model="preferencesForm.theme" @change="updateTheme(preferencesForm.theme)" :disabled="isLoading"
                    class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-800">
                    <option v-for="option in themeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                  </select>
                </div>

                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Formato Orario</label>
                  <select v-model="preferencesForm.timeFormat" @change="updateTimeFormat" :disabled="isLoading"
                    class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-800">
                    <option value="24h">24 ore (15:30)</option>
                    <option value="12h">12 ore (3:30 PM)</option>
                  </select>
                </div>

                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Fuso Orario</label>
                  <input v-model="preferencesForm.timezone" @change="updateTimezone" :disabled="isLoading"
                    class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-800" />
                </div>

                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Inizio Settimana</label>
                  <select v-model.number="preferencesForm.weekStartDay" @change="updateWeekStartDay" :disabled="isLoading"
                    class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-800">
                    <option :value="1">Lunedì</option>
                    <option :value="0">Domenica</option>
                  </select>
                </div>
              </div>

              <NotificationSettings />
            </div>

            <!-- Mobile: Danger Zone -->
            <div v-else-if="tab.id === 'danger'" class="space-y-4">
              <div class="bg-red-50 dark:bg-red-900/20 p-4 rounded-lg">
                <h4 class="text-sm font-medium text-red-900 dark:text-red-200 mb-2">Esporta i Tuoi Dati</h4>
                <p class="text-xs text-red-700 dark:text-red-300 mb-3">
                  Scarica una copia completa dei tuoi dati in formato JSON.
                </p>
                <button @click="exportData" :disabled="isLoading"
                  class="w-full px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 text-sm disabled:opacity-50">
                  <LoadingSpinner v-if="isLoading" class="w-4 h-4 mr-2 inline-block" />
                  {{ isLoading ? 'Esportando...' : 'Esporta Dati' }}
                </button>
              </div>

              <div class="bg-red-50 dark:bg-red-900/20 p-4 rounded-lg">
                <h4 class="text-sm font-medium text-red-900 dark:text-red-200 mb-2">Elimina Account</h4>
                <p class="text-xs text-red-700 dark:text-red-300 mb-3">
                  Una volta eliminato, non potrai recuperare il tuo account.
                </p>
                <button @click="showDeleteAccountModal = true" :disabled="isLoading"
                  class="w-full px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 text-sm disabled:opacity-50">
                  Elimina Account
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Tab Content (Desktop only) -->
      <div class="hidden md:block p-6">
        <!-- Personal Information Tab -->
        <div v-if="activeTab === 'personal'" class="space-y-6">
          <div class="flex items-center justify-between">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
              Informazioni Personali
            </h3>
            <button
              @click="toggleEditMode('personal')"
              class="inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              <svg v-if="!editModes.personal" class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
              <svg v-else class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
              {{ editModes.personal ? 'Annulla' : 'Modifica' }}
            </button>
          </div>

          <form @submit.prevent="handlePersonalInfoSave" class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <!-- Username -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Nome Utente
              </label>
              <input
                v-model="profileForm.username"
                type="text"
                disabled
                class="w-full px-4 py-3 border rounded-lg bg-gray-50 dark:bg-gray-800 text-gray-500 dark:text-gray-400 cursor-not-allowed"
                placeholder="Nome utente"
              />
              <p class="mt-1 text-sm text-gray-500 dark:text-gray-400" v-if="editModes.personal">
                Il nome utente non può essere modificato
              </p>
            </div>

            <!-- Email -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Email
              </label>
              <input
                v-model="profileForm.email"
                type="email"
                :disabled="user?.emailVerified || !editModes.personal || isLoading"
                :class="[
                  'w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
                  user?.emailVerified || !editModes.personal
                    ? 'bg-gray-50 dark:bg-gray-800 text-gray-500 dark:text-gray-400 cursor-not-allowed'
                    : 'bg-white dark:bg-gray-800 border-gray-300 dark:border-gray-600'
                ]"
                placeholder="Indirizzo email"
              />
              <p v-if="user?.emailVerified" class="mt-1 text-sm text-green-500 dark:text-green-400">
                ✓ Email verificata e protetta
              </p>
              <p v-else class="mt-1 text-sm text-yellow-600 dark:text-yellow-400"  v-if="editModes.personal">
                ⚠ Email non verificata o verifica non richiesta
              </p>
              <p v-if="errors.email" class="mt-1 text-sm text-red-600 dark:text-red-400">
                {{ errors.email }}
              </p>
            </div>

            <!-- First Name -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Nome
              </label>
              <input
                :value="editModes.personal ? profileForm.firstName : (user?.firstName || '')"
                @input="editModes.personal && (profileForm.firstName = ($event.target as HTMLInputElement).value)"
                type="text"
                :disabled="!editModes.personal || isLoading"
                :class="[
                  'w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
                  !editModes.personal
                    ? 'bg-gray-50 dark:bg-gray-800 text-gray-500 dark:text-gray-400 cursor-not-allowed'
                    : 'bg-white dark:bg-gray-800 border-gray-300 dark:border-gray-600'
                ]"
                :placeholder="editModes.personal ? 'Inserisci il tuo nome' : 'Nome non specificato'"
              />
            </div>

            <!-- Last Name -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Cognome
              </label>
              <input
                :value="editModes.personal ? profileForm.lastName : (user?.lastName || '')"
                @input="editModes.personal && (profileForm.lastName = ($event.target as HTMLInputElement).value)"
                type="text"
                :disabled="!editModes.personal || isLoading"
                :class="[
                  'w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
                  !editModes.personal
                    ? 'bg-gray-50 dark:bg-gray-800 text-gray-500 dark:text-gray-400 cursor-not-allowed'
                    : 'bg-white dark:bg-gray-800 border-gray-300 dark:border-gray-600'
                ]"
                :placeholder="editModes.personal ? 'Inserisci il tuo cognome' : 'Cognome non specificato'"
              />
            </div>

            <!-- Save Button -->
            <div v-if="editModes.personal" class="md:col-span-2">
              <div class="flex justify-end space-x-4">
                <button
                  type="button"
                  @click="cancelPersonalEdit"
                  :disabled="isLoading"
                  class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
                >
                  Annulla
                </button>
                <button
                  type="submit"
                  :disabled="isLoading"
                  class="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
                >
                  <LoadingSpinner v-if="isLoading" class="w-4 h-4 mr-2" />
                  {{ isLoading ? 'Salvando...' : 'Salva Modifiche' }}
                </button>
              </div>
            </div>
          </form>
        </div>

        <!-- Security Tab -->
        <div v-else-if="activeTab === 'security'" class="space-y-8">
          <!-- Change Password Section -->
          <div>
            <div class="flex items-center justify-between mb-6">
              <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
                Cambia Password
              </h3>
              <button
                @click="toggleEditMode('password')"
                class="inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
              >
                <svg v-if="!editModes.password" class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                </svg>
                {{ editModes.password ? 'Annulla' : 'Modifica Password' }}
              </button>
            </div>

            <form v-if="editModes.password" @submit.prevent="handlePasswordChange" class="space-y-6 max-w-md">
              <!-- Current Password -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Password Attuale *
                </label>
                <div class="relative">
                  <input
                    v-model="passwordForm.currentPassword"
                    :type="showPasswords.current ? 'text' : 'password'"
                    required
                    :disabled="isLoading"
                    class="w-full px-4 py-3 pr-12 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-800 transition-colors"
                    placeholder="Inserisci password attuale"
                  />
                  <button
                    type="button"
                    @click="showPasswords.current = !showPasswords.current"
                    class="absolute inset-y-0 right-0 pr-3 flex items-center"
                    :disabled="isLoading"
                  >
                    <svg 
                      v-if="showPasswords.current" 
                      class="h-5 w-5 text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300" 
                      fill="none" 
                      stroke="currentColor" 
                      viewBox="0 0 24 24"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.878 9.878L3 3m6.878 6.878L21 21" />
                    </svg>
                    <svg 
                      v-else 
                      class="h-5 w-5 text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300" 
                      fill="none" 
                      stroke="currentColor" 
                      viewBox="0 0 24 24"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                  </button>
                </div>
                <p v-if="errors.currentPassword" class="mt-1 text-sm text-red-600 dark:text-red-400">
                  {{ errors.currentPassword }}
                </p>
              </div>

              <!-- New Password -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Nuova Password *
                </label>
                <div class="relative">
                  <input
                    v-model="passwordForm.newPassword"
                    :type="showPasswords.new ? 'text' : 'password'"
                    required
                    :disabled="isLoading"
                    class="w-full px-4 py-3 pr-12 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-800 transition-colors"
                    placeholder="Inserisci nuova password"
                    @input="validateNewPassword"
                  />
                  <button
                    type="button"
                    @click="showPasswords.new = !showPasswords.new"
                    class="absolute inset-y-0 right-0 pr-3 flex items-center"
                    :disabled="isLoading"
                  >
                    <svg 
                      v-if="showPasswords.new" 
                      class="h-5 w-5 text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300" 
                      fill="none" 
                      stroke="currentColor" 
                      viewBox="0 0 24 24"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.878 9.878L3 3m6.878 6.878L21 21" />
                    </svg>
                    <svg 
                      v-else 
                      class="h-5 w-5 text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300" 
                      fill="none" 
                      stroke="currentColor" 
                      viewBox="0 0 24 24"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                  </button>
                </div>
                <p v-if="errors.newPassword" class="mt-1 text-sm text-red-600 dark:text-red-400">
                  {{ errors.newPassword }}
                </p>
                
                <!-- Password Strength Indicator -->
                <div v-if="passwordForm.newPassword" class="mt-2">
                  <div class="flex items-center space-x-2">
                    <span class="text-sm text-gray-500 dark:text-gray-400">Sicurezza:</span>
                    <div class="flex-1 bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                      <div 
                        :class="[
                          'h-2 rounded-full transition-all duration-300',
                          newPasswordStrength.level === 'weak' ? 'bg-red-500 w-1/4' :
                          newPasswordStrength.level === 'medium' ? 'bg-yellow-500 w-2/4' :
                          newPasswordStrength.level === 'good' ? 'bg-blue-500 w-3/4' :
                          newPasswordStrength.level === 'strong' ? 'bg-green-500 w-full' : 'bg-gray-300 w-0'
                        ]"
                      ></div>
                    </div>
                    <span 
                      :class="[
                        'text-sm font-medium',
                        newPasswordStrength.level === 'weak' ? 'text-red-500' :
                        newPasswordStrength.level === 'medium' ? 'text-yellow-500' :
                        newPasswordStrength.level === 'good' ? 'text-blue-500' :
                        newPasswordStrength.level === 'strong' ? 'text-green-500' : 'text-gray-400'
                      ]"
                    >
                      {{ newPasswordStrength.text }}
                    </span>
                  </div>
                </div>
              </div>

              <!-- Confirm New Password -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Conferma Nuova Password *
                </label>
                <div class="relative">
                  <input
                    v-model="passwordForm.confirmPassword"
                    :type="showPasswords.confirm ? 'text' : 'password'"
                    required
                    :disabled="isLoading"
                    class="w-full px-4 py-3 pr-12 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-800 transition-colors"
                    placeholder="Conferma nuova password"
                    @blur="validateConfirmPassword"
                  />
                  <button
                    type="button"
                    @click="showPasswords.confirm = !showPasswords.confirm"
                    class="absolute inset-y-0 right-0 pr-3 flex items-center"
                    :disabled="isLoading"
                  >
                    <svg 
                      v-if="showPasswords.confirm" 
                      class="h-5 w-5 text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300" 
                      fill="none" 
                      stroke="currentColor" 
                      viewBox="0 0 24 24"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.878 9.878L3 3m6.878 6.878L21 21" />
                    </svg>
                    <svg 
                      v-else 
                      class="h-5 w-5 text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300" 
                      fill="none" 
                      stroke="currentColor" 
                      viewBox="0 0 24 24"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                  </button>
                </div>
                <p v-if="errors.confirmPassword" class="mt-1 text-sm text-red-600 dark:text-red-400">
                  {{ errors.confirmPassword }}
                </p>
              </div>

              <!-- Password Change Actions -->
              <div class="flex justify-end space-x-4">
                <button
                  type="button"
                  @click="cancelPasswordEdit"
                  :disabled="isLoading"
                  class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
                >
                  Annulla
                </button>
                <button
                  type="submit"
                  :disabled="isLoading || !isPasswordFormValid"
                  class="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
                >
                  <LoadingSpinner v-if="isLoading" class="w-4 h-4 mr-2" />
                  {{ isLoading ? 'Cambiando...' : 'Cambia Password' }}
                </button>
              </div>
            </form>
          </div>

          <!-- Two-Factor Authentication Section -->
          <div class="border-t border-gray-200 dark:border-gray-700 pt-8">
            <div class="flex items-center justify-between">
              <div>
                <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
                  Autenticazione a Due Fattori
                </h3>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  Aumenta la sicurezza del tuo account abilitando l'autenticazione a due fattori
                </p>
              </div>
              <button
                @click="toggle2FA"
                :disabled="isLoading"
                class="inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
              >
                {{ user?.twoFactorEnabled ? 'Disabilita 2FA' : 'Abilita 2FA' }}
              </button>
            </div>
          </div>
        </div>

        <!-- Preferences Tab -->
        <div v-else-if="activeTab === 'preferences'" class="space-y-8">
            <!-- Theme Preference -->
            <div>
              <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">
                Tema Applicazione
              </h3>
              <div class="flex items-center justify-between">
                <div>
                  <h4 class="text-sm font-medium text-gray-900 dark:text-white">
                    Modalità tema
                  </h4>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    Scegli come visualizzare l'interfaccia
                  </p>
                </div>
                <div class="flex items-center space-x-2">
                  <button
                    v-for="themeOption in themeOptions"
                    :key="themeOption.value"
                    type="button"
                    @click="updateTheme(themeOption.value)"
                    :disabled="isLoading"
                    :class="[
                      'p-2 rounded transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500',
                      preferencesForm.theme === themeOption.value
                        ? 'bg-blue-100 text-blue-600 dark:bg-blue-900 dark:text-blue-400'
                        : 'text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'
                    ]"
                    :title="themeOption.label"
                  >
                    <component :is="themeOption.icon" class="w-5 h-5" />
                  </button>
                </div>
              </div>
            </div>

            <!-- App Preferences -->
            <div>
              <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">
                Preferenze Applicazione
              </h3>
              <div class="space-y-6">
                <!-- Time Format -->
                <div class="flex items-center justify-between">
                  <div>
                    <h4 class="text-sm font-medium text-gray-900 dark:text-white">
                      Formato orario
                    </h4>
                    <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                      Formato di visualizzazione degli orari
                    </p>
                  </div>
                  <select
                    v-model="preferencesForm.timeFormat"
                    @change="updateTimeFormat"
                    :disabled="isLoading"
                    class="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="24h">24 ore (15:30)</option>
                    <option value="12h">12 ore (3:30 PM)</option>
                  </select>
                </div>

                <!-- Default Calendar View -->
                <div class="flex items-center justify-between">
                  <div>
                    <h4 class="text-sm font-medium text-gray-900 dark:text-white">
                      Visualizzazione predefinita
                    </h4>
                    <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                      Vista che si apre all'avvio dell'applicazione
                    </p>
                  </div>
                  <select
                    v-model="preferencesForm.calendarView"
                    @change="updateCalendarView"
                    :disabled="isLoading"
                    class="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="week">Settimana</option>
                    <option value="month">Mese</option>
                    <option value="day">Giorno</option>
                    <option value="agenda">Agenda</option>
                  </select>
                </div>

                <!-- Timezone -->
                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Fuso Orario
                  </label>
                  <select
                    v-model="preferencesForm.timezone"
                    @change="updateTimezone"
                    :disabled="isLoading"
                    class="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-800 text-gray-900 dark:text-white transition-colors"
                  >
                    <optgroup label="Italia">
                      <option value="Europe/Rome">Roma (UTC+1)</option>
                    </optgroup>
                    <optgroup label="Europa">
                      <option value="Europe/London">Londra (UTC+0)</option>
                      <option value="Europe/Berlin">Berlino (UTC+1)</option>
                      <option value="Europe/Paris">Parigi (UTC+1)</option>
                      <option value="Europe/Madrid">Madrid (UTC+1)</option>
                      <option value="Europe/Amsterdam">Amsterdam (UTC+1)</option>
                    </optgroup>
                    <optgroup label="Americhe">
                      <option value="America/New_York">New York (UTC-5)</option>
                      <option value="America/Los_Angeles">Los Angeles (UTC-8)</option>
                      <option value="America/Chicago">Chicago (UTC-6)</option>
                    </optgroup>
                    <optgroup label="Asia">
                      <option value="Asia/Tokyo">Tokyo (UTC+9)</option>
                      <option value="Asia/Shanghai">Shanghai (UTC+8)</option>
                      <option value="Asia/Dubai">Dubai (UTC+4)</option>
                    </optgroup>
                  </select>
                </div>

                <!-- Week Start Day -->
                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Inizio Settimana
                  </label>
                  <select
                    v-model="preferencesForm.weekStartDay"
                    @change="updateWeekStartDay"
                    :disabled="isLoading"
                    class="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-800 text-gray-900 dark:text-white transition-colors"
                  >
                    <option :value="1">Lunedì</option>
                    <option :value="0">Domenica</option>
                  </select>
                </div>
              </div>
            </div>

            <!-- Notification Preferences -->
            <div>
              <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">
                Preferenze Notifiche
              </h3>
              <div class="space-y-4">
                <div class="flex items-center justify-between">
                  <div>
                    <label for="email-notifications" class="text-sm font-medium text-gray-700 dark:text-gray-300">
                      Notifiche Email
                    </label>
                    <p class="text-sm text-gray-500 dark:text-gray-400">
                      Ricevi notifiche via email per promemoria importanti
                    </p>
                  </div>
                  <input
                    id="email-notifications"
                    v-model="preferencesForm.emailNotifications"
                    @change="updateEmailNotifications"
                    type="checkbox"
                    :disabled="isLoading"
                    class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                </div>

                <div class="flex items-center justify-between">
                  <div>
                    <label for="reminder-notifications" class="text-sm font-medium text-gray-700 dark:text-gray-300">
                      Notifiche Promemoria
                    </label>
                    <p class="text-sm text-gray-500 dark:text-gray-400">
                      Ricevi notifiche push per i tuoi promemoria
                    </p>
                  </div>
                  <input
                    id="reminder-notifications"
                    v-model="preferencesForm.reminderNotifications"
                    @change="updateReminderNotifications"
                    type="checkbox"
                    :disabled="isLoading"
                    class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                </div>
              </div>
            </div>

            <!-- Advanced Notifications Section -->
            <div>
              <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">
                Notifiche Avanzate
              </h3>
              <div class="bg-gray-50 dark:bg-gray-800 rounded-lg p-4">
                <NotificationSettings />
              </div>
            </div>

        </div>

        <!-- Danger Zone Tab -->
        <div v-else-if="activeTab === 'danger'" class="space-y-8">
          <!-- Export Data -->
          <div class="border border-gray-200 dark:border-gray-700 rounded-lg p-6">
            <div class="flex items-center justify-between">
              <div>
                <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
                  Esporta Dati
                </h3>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  Scarica una copia di tutti i tuoi dati (GDPR compliance)
                </p>
              </div>
              <button
                @click="exportData"
                :disabled="isLoading"
                class="inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
              >
                <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                Esporta Dati
              </button>
            </div>
          </div>

          <!-- Delete Account -->
          <div class="border border-red-200 dark:border-red-800 rounded-lg p-6 bg-red-50 dark:bg-red-900/20">
            <div class="flex items-center justify-between">
              <div>
                <h3 class="text-lg font-semibold text-red-900 dark:text-red-400">
                  Elimina Account
                </h3>
                <p class="text-sm text-red-700 dark:text-red-300 mt-1">
                  Elimina permanentemente il tuo account e tutti i dati associati. Questa azione non può essere annullata.
                </p>
              </div>
              <button
                @click="showDeleteAccountModal = true"
                :disabled="isLoading"
                class="inline-flex items-center px-4 py-2 border border-red-300 dark:border-red-600 rounded-md shadow-sm text-sm font-medium text-red-700 dark:text-red-300 bg-white dark:bg-red-900/50 hover:bg-red-50 dark:hover:bg-red-900/70 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 disabled:opacity-50"
              >
                <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
                Elimina Account
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Two-Factor Setup Modal -->
    <TwoFactorSetupModal
      v-model="showTwoFactorSetupModal"
      @success="handle2FASetupSuccess"
    />

    <!-- Two-Factor Disable Modal -->
    <TwoFactorDisableModal
      v-model="showTwoFactorDisableModal"
      @success="handle2FADisableSuccess"
    />

    <!-- Delete Account Modal -->
    <Modal
      v-model="showDeleteAccountModal"
      title="Conferma Eliminazione Account"
      :persistent="true"
    >
      <div class="space-y-4">
        <div class="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-700 rounded-lg p-4">
          <div class="flex">
            <svg class="h-5 w-5 text-red-400" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
            </svg>
            <div class="ml-3">
              <h3 class="text-sm font-medium text-red-800 dark:text-red-200">
                Attenzione: Questa azione è irreversibile
              </h3>
              <div class="mt-2 text-sm text-red-700 dark:text-red-300">
                <ul class="list-disc pl-5 space-y-1">
                  <li>Tutti i tuoi dati verranno eliminati permanentemente</li>
                  <li>Le tue attività, promemoria e impostazioni andranno perse</li>
                  <li>Non sarà possibile recuperare l'account una volta eliminato</li>
                </ul>
              </div>
            </div>
          </div>
        </div>

        <p class="text-sm text-gray-600 dark:text-gray-400">
          Per confermare l'eliminazione, inserisci la tua password attuale:
        </p>

        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Password Attuale
          </label>
          <input
            v-model="deleteAccountPassword"
            type="password"
            required
            :disabled="isLoading"
            class="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent bg-white dark:bg-gray-800 transition-colors"
            placeholder="Inserisci la tua password"
          />
          <p v-if="deleteAccountError" class="mt-1 text-sm text-red-600 dark:text-red-400">
            {{ deleteAccountError }}
          </p>
        </div>

        <div class="flex justify-end space-x-4 pt-4">
          <button
            @click="cancelDeleteAccount"
            :disabled="isLoading"
            class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
          >
            Annulla
          </button>
          <button
            @click="confirmDeleteAccount"
            :disabled="isLoading || !deleteAccountPassword.trim()"
            class="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 disabled:opacity-50"
          >
            <LoadingSpinner v-if="isLoading" class="w-4 h-4 mr-2" />
            {{ isLoading ? 'Eliminando...' : 'Elimina Account' }}
          </button>
        </div>
      </div>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useCustomToast } from '@/composables/useCustomToast'
import { useTheme } from '@/composables/useTheme'
import { useSettingsStore } from '@/stores/settings'
import { authApi } from '@/services/authApi'
import LoadingSpinner from '@/components/Common/LoadingSpinner.vue'
import Modal from '@/components/Common/Modal.vue'
import NotificationSettings from '@/components/Reminder/NotificationSettings.vue'
import TwoFactorSetupModal from '@/components/Auth/TwoFactorSetupModal.vue'
import TwoFactorDisableModal from '@/components/Auth/TwoFactorDisableModal.vue'
import { SunIcon, MoonIcon, ComputerDesktopIcon, UserIcon, ShieldCheckIcon, Cog6ToothIcon, ExclamationTriangleIcon } from '@heroicons/vue/24/outline'
import type { User } from '@/types/auth'

// Composables
const { user, userFullName, userInitials, updateProfile, logout, isLoading } = useAuth()
const { showError, showSuccess } = useCustomToast()
const { themeMode: currentTheme, setThemeMode } = useTheme()
const settingsStore = useSettingsStore()

// Tab icons
const PersonIcon = UserIcon
const SecurityIcon = ShieldCheckIcon
const SettingsIcon = Cog6ToothIcon
const DangerIcon = ExclamationTriangleIcon

// Format member since date
const formatMemberSince = (dateString: string | Date): string => {
  if (!dateString) return ''

  try {
    const date = new Date(dateString)
    return date.toLocaleDateString('it-IT', {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    })
  } catch (error) {
    console.error('Error formatting member since date:', error)
    return ''
  }
}

// Theme options
const themeOptions = [
  { value: 'light', label: 'Modalità chiara', icon: SunIcon },
  { value: 'dark', label: 'Modalità scura', icon: MoonIcon },
  { value: 'system', label: 'Segui sistema', icon: ComputerDesktopIcon }
]

// Component state
const activeTab = ref('personal')

// Mobile accordion toggle
const toggleMobileSection = (tabId: string) => {
  if (activeTab.value === tabId) {
    activeTab.value = null
  } else {
    activeTab.value = tabId
  }
}
// const avatarInput = ref<HTMLInputElement>() // TODO: Uncomment when avatar upload is implemented
const showDeleteAccountModal = ref(false)
const deleteAccountPassword = ref('')
const deleteAccountError = ref('')
const showTwoFactorSetupModal = ref(false)
const showTwoFactorDisableModal = ref(false)

// Edit modes
const editModes = reactive({
  personal: false,
  password: false
})

// Show password states
const showPasswords = reactive({
  current: false,
  new: false,
  confirm: false
})

// Form states
const profileForm = reactive({
  username: '',
  email: '',
  firstName: '',
  lastName: ''
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const preferencesForm = reactive({
  theme: 'system' as 'light' | 'dark' | 'system',
  timezone: 'Europe/Rome',
  timeFormat: '24h' as '12h' | '24h',
  calendarView: 'week' as 'month' | 'week' | 'day' | 'agenda',
  emailNotifications: true,
  reminderNotifications: true,
  weekStartDay: 1 as 0 | 1
})

// Error states
const errors = reactive({
  username: '',
  email: '',
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// Tabs configuration
const tabs = [
  { id: 'personal', name: 'Informazioni Personali', icon: PersonIcon },
  { id: 'security', name: 'Sicurezza', icon: SecurityIcon },
  { id: 'preferences', name: 'Preferenze', icon: SettingsIcon },
  { id: 'danger', name: 'Area Pericolo', icon: DangerIcon }
]

// Computed properties
const newPasswordStrength = computed(() => {
  const password = passwordForm.newPassword
  if (!password) return { level: '', text: '', suggestions: [] }

  let score = 0
  const suggestions = []

  // Length check
  if (password.length >= 8) {
    score += 1
  } else {
    suggestions.push('Almeno 8 caratteri')
  }

  // Complexity checks
  if (/[a-z]/.test(password)) score += 1
  else suggestions.push('Lettere minuscole')

  if (/[A-Z]/.test(password)) score += 1
  else suggestions.push('Lettere maiuscole')

  if (/[0-9]/.test(password)) score += 1
  else suggestions.push('Numeri')

  if (/[^a-zA-Z0-9]/.test(password)) score += 1
  else suggestions.push('Caratteri speciali (!@#$%^&*)')

  // Special patterns
  if (password.length >= 12) score += 1

  const level = score <= 1 ? 'weak' : score <= 2 ? 'medium' : score <= 3 ? 'good' : 'strong'
  const text = score <= 1 ? 'Debole' : score <= 2 ? 'Media' : score <= 3 ? 'Buona' : 'Forte'

  return { level, text, suggestions }
})

const isPasswordFormValid = computed(() => {
  return passwordForm.currentPassword.trim().length > 0 &&
         passwordForm.newPassword.trim().length >= 8 &&
         passwordForm.confirmPassword === passwordForm.newPassword &&
         newPasswordStrength.value.level !== 'weak' &&
         !errors.currentPassword &&
         !errors.newPassword &&
         !errors.confirmPassword
})

// Utility methods
const formatDate = (dateString?: string): string => {
  if (!dateString) return 'Data non disponibile'
  
  try {
    const date = new Date(dateString)
    return date.toLocaleDateString('it-IT', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    })
  } catch {
    return 'Data non valida'
  }
}

// Form methods
const toggleEditMode = (mode: keyof typeof editModes) => {
  editModes[mode] = !editModes[mode]
  
  if (mode === 'personal' && editModes.personal) {
    // Initialize form with current user data
    initializeProfileForm()
  } else if (mode === 'password' && editModes.password) {
    // Reset password form
    resetPasswordForm()
  }
}

const initializeProfileForm = () => {
  profileForm.username = user.value?.username || ''
  profileForm.email = user.value?.email || ''
  profileForm.firstName = user.value?.firstName || ''
  profileForm.lastName = user.value?.lastName || ''
}

const resetPasswordForm = () => {
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  Object.keys(errors).forEach(key => {
    if (key.includes('Password')) {
      errors[key as keyof typeof errors] = ''
    }
  })
}

// Validation methods
const validateNewPassword = () => {
  if (!passwordForm.newPassword.trim()) {
    errors.newPassword = 'La nuova password è richiesta'
  } else if (passwordForm.newPassword.length < 8) {
    errors.newPassword = 'La password deve contenere almeno 8 caratteri'
  } else if (newPasswordStrength.value.level === 'weak') {
    errors.newPassword = 'La password è troppo debole'
  } else if (passwordForm.newPassword === passwordForm.currentPassword) {
    errors.newPassword = 'La nuova password deve essere diversa da quella attuale'
  } else {
    errors.newPassword = ''
  }
}

const validateConfirmPassword = () => {
  if (!passwordForm.confirmPassword.trim()) {
    errors.confirmPassword = 'Conferma la password'
  } else if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    errors.confirmPassword = 'Le password non coincidono'
  } else {
    errors.confirmPassword = ''
  }
}

// Avatar handling - TODO: Implement backend avatar upload
// const triggerAvatarUpload = () => {
//   avatarInput.value?.click()
// }

// const handleAvatarChange = async (event: Event) => {
//   const target = event.target as HTMLInputElement
//   const file = target.files?.[0]
//
//   if (!file) return

//   // Validate file
//   if (!file.type.startsWith('image/')) {
//     showError('Seleziona un file immagine valido')
//     return
//   }
//
//   if (file.size > 5 * 1024 * 1024) { // 5MB limit
//     showError('Il file è troppo grande. Massimo 5MB consentito')
//     return
//   }

//   try {
//     // Create form data for upload
//     const formData = new FormData()
//     formData.append('avatar', file)
//
//     // TODO: Implement avatar upload API call
//     showSuccess('Avatar aggiornato con successo!')
//   } catch (error) {
//     console.error('Avatar upload failed:', error)
//     showError('Errore durante l\'aggiornamento dell\'avatar')
//   }
// }

// Form submission handlers
const handlePersonalInfoSave = async () => {
  try {
    // Note: username cannot be modified. Email can only be modified if not verified.
    const updateData: Partial<User> = {
      firstName: profileForm.firstName.trim() || undefined,
      lastName: profileForm.lastName.trim() || undefined
    }

    // Include email only if it's not verified (allowing correction of typos)
    if (!user.value?.emailVerified) {
      updateData.email = profileForm.email.trim()
    }
    
    const success = await updateProfile(updateData)
    if (success) {
      editModes.personal = false
      showSuccess('Informazioni personali aggiornate con successo!')
    }
  } catch (error) {
    console.error('Profile update failed:', error)
    showError('Errore durante l\'aggiornamento del profilo')
  }
}

const handlePasswordChange = async () => {
  if (!isPasswordFormValid.value) return

  try {
    await authApi.changePassword(
      passwordForm.currentPassword,
      passwordForm.newPassword
    )
    
    editModes.password = false
    resetPasswordForm()
    showSuccess('Password cambiata con successo!')
  } catch (error: any) {
    console.error('Password change failed:', error)
    
    if (error.response?.status === 401) {
      errors.currentPassword = 'Password attuale non corretta'
    } else {
      showError('Errore durante il cambio password')
    }
  }
}

// Auto-save individual preference updates
const savePreference = async (updates: Partial<typeof preferencesForm>) => {
  try {
    const updatedPreferences = await authApi.updatePreferences(updates)

    // Update settings store with new values
    if (updates.theme) {
      setThemeMode(updates.theme as 'light' | 'dark' | 'system')
      settingsStore.updateTheme(updates.theme)
    }
    if (updates.timeFormat) {
      settingsStore.updateTimeFormat(updates.timeFormat)
    }
    if (updates.calendarView) {
      settingsStore.updateCalendarView(updates.calendarView)
    }

    // Update form with server response to ensure consistency
    if (updatedPreferences) {
      Object.assign(preferencesForm, updatedPreferences)
    }

  } catch (error) {
    console.error('Preference save failed:', error)
    showError('Errore durante il salvataggio della preferenza')
  }
}

// Individual preference update functions
const updateTheme = (theme: string) => {
  preferencesForm.theme = theme as 'light' | 'dark' | 'system'
  savePreference({ theme: preferencesForm.theme })
}

const updateTimeFormat = () => {
  savePreference({ timeFormat: preferencesForm.timeFormat })
}

const updateCalendarView = () => {
  savePreference({ calendarView: preferencesForm.calendarView })
}

const updateTimezone = () => {
  savePreference({ timezone: preferencesForm.timezone })
}

const updateEmailNotifications = () => {
  savePreference({ emailNotifications: preferencesForm.emailNotifications })
}

const updateReminderNotifications = () => {
  savePreference({ reminderNotifications: preferencesForm.reminderNotifications })
}

const updateWeekStartDay = () => {
  savePreference({ weekStartDay: preferencesForm.weekStartDay })
  settingsStore.updateWeekStartDay(preferencesForm.weekStartDay)
}

// Cancel handlers
const cancelPersonalEdit = () => {
  editModes.personal = false
  initializeProfileForm()
  Object.keys(errors).forEach(key => {
    if (!key.includes('Password')) {
      errors[key as keyof typeof errors] = ''
    }
  })
}

const cancelPasswordEdit = () => {
  editModes.password = false
  resetPasswordForm()
}

// Two-Factor Authentication
const toggle2FA = () => {
  if (user.value?.twoFactorEnabled) {
    showTwoFactorDisableModal.value = true
  } else {
    showTwoFactorSetupModal.value = true
  }
}

const handle2FASetupSuccess = async () => {
  try {
    const updatedUser = await authApi.getProfile()
    if (user.value) {
      user.value.twoFactorEnabled = updatedUser.twoFactorEnabled
    }
  } catch (error) {
    console.error('Error refreshing user profile:', error)
  }
}

const handle2FADisableSuccess = async () => {
  try {
    const updatedUser = await authApi.getProfile()
    if (user.value) {
      user.value.twoFactorEnabled = updatedUser.twoFactorEnabled
    }
  } catch (error) {
    console.error('Error refreshing user profile:', error)
  }
}

// Data export
const exportData = async () => {
  try {
    const { blob, filename } = await authApi.exportData()

    // Create download link
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    showSuccess('Dati esportati con successo!')
  } catch (error) {
    console.error('Data export failed:', error)
    showError('Errore durante l\'esportazione dei dati')
  }
}

// Account deletion
const cancelDeleteAccount = () => {
  showDeleteAccountModal.value = false
  deleteAccountPassword.value = ''
  deleteAccountError.value = ''
}

const confirmDeleteAccount = async () => {
  if (!deleteAccountPassword.value.trim()) return

  try {
    deleteAccountError.value = ''
    
    await authApi.deleteAccount(deleteAccountPassword.value)
    
    showSuccess('Account eliminato con successo')
    
    // Logout and redirect
    await logout()
  } catch (error: any) {
    console.error('Account deletion failed:', error)
    
    if (error.response?.status === 401) {
      deleteAccountError.value = 'Password non corretta'
    } else {
      deleteAccountError.value = 'Errore durante l\'eliminazione dell\'account'
    }
  }
}

// Load preferences on mount
const loadPreferences = async () => {
  try {
    const preferences = await authApi.getPreferences()
    preferencesForm.theme = preferences.theme || 'system'
    preferencesForm.timezone = preferences.timezone || 'Europe/Rome'
    preferencesForm.timeFormat = preferences.timeFormat || '24h'
    preferencesForm.calendarView = preferences.calendarView || 'week'
    preferencesForm.emailNotifications = preferences.emailNotifications ?? true
    preferencesForm.reminderNotifications = preferences.reminderNotifications ?? true
    preferencesForm.weekStartDay = preferences.weekStartDay ?? 1

    // Sync with settings store
    settingsStore.updateTheme(preferencesForm.theme)
    settingsStore.updateTimeFormat(preferencesForm.timeFormat)
    settingsStore.updateCalendarView(preferencesForm.calendarView)
    settingsStore.updateWeekStartDay(preferencesForm.weekStartDay)
  } catch (error) {
    console.error('Failed to load preferences:', error)
    // Load defaults from settings store
    preferencesForm.theme = settingsStore.settings.theme
    preferencesForm.timeFormat = settingsStore.settings.timeFormat
    preferencesForm.calendarView = settingsStore.settings.calendarView
  }
}

// Lifecycle
onMounted(() => {
  initializeProfileForm()
  loadPreferences()
})
</script>

<style scoped>
.user-profile {
  @apply max-w-4xl mx-auto;
}

/* Custom focus styles */
input:focus,
select:focus {
  @apply outline-none ring-2 ring-blue-500 ring-offset-2 dark:ring-offset-gray-900;
}

/* Tab button focus styles - remove all focus styles */
nav button {
  @apply focus:outline-none focus:ring-0 focus:shadow-none;
}

nav button:focus {
  outline: none !important;
  box-shadow: none !important;
  border: none !important;
}

/* Tab transitions */
.tab-content {
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Avatar hover effect */
.group:hover .group-hover\:opacity-100 {
  opacity: 1;
}

/* Mobile responsiveness */
@media (max-width: 768px) {
  .user-profile {
    @apply mx-4;
  }
  
  
  .space-x-8 {
    @apply space-x-4;
  }
}

/* High contrast mode */
@media (prefers-contrast: high) {
  .border-gray-200 {
    @apply border-black;
  }
  
  .text-gray-600 {
    @apply text-black;
  }
}

/* Print styles */
@media print {
  .user-profile {
    @apply shadow-none;
  }
  
  button {
    @apply hidden;
  }
}
</style>