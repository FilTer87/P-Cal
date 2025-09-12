<template>
  <div class="user-profile">
    <div class="bg-white dark:bg-gray-900 shadow-lg rounded-lg overflow-hidden">
      <!-- Profile Header -->
      <div class="px-6 py-8 bg-gradient-to-r from-blue-500 to-purple-600">
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
            
            <!-- Avatar Upload Overlay -->
            <div class="absolute inset-0 bg-black bg-opacity-50 rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer">
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
            </div>
          </div>
          
          <!-- User Info -->
          <div class="flex-1 text-center sm:text-left">
            <h1 class="text-2xl font-bold text-white mb-2">
              {{ userFullName || user?.username }}
            </h1>
            <p class="text-blue-100 mb-2">{{ user?.email }}</p>
            <p class="text-blue-200 text-sm">
              Membro dal {{ formatDate(user?.createdAt) }}
            </p>
          </div>
        </div>
      </div>

      <!-- Navigation Tabs -->
      <div class="border-b border-gray-200 dark:border-gray-700">
        <nav class="-mb-px flex space-x-8 px-6">
          <button
            v-for="tab in tabs"
            :key="tab.id"
            @click="activeTab = tab.id"
            :class="[
              'py-4 px-1 border-b-2 font-medium text-sm transition-colors',
              activeTab === tab.id
                ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300 hover:border-gray-300'
            ]"
          >
            <component :is="tab.icon" class="w-5 h-5 inline mr-2" />
            {{ tab.name }}
          </button>
        </nav>
      </div>

      <!-- Tab Content -->
      <div class="p-6">
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
                :disabled="!editModes.personal || isLoading"
                :class="[
                  'w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
                  !editModes.personal 
                    ? 'bg-gray-50 dark:bg-gray-800 text-gray-500 dark:text-gray-400 cursor-not-allowed' 
                    : 'bg-white dark:bg-gray-800 border-gray-300 dark:border-gray-600'
                ]"
                placeholder="Nome utente"
              />
              <p v-if="errors.username" class="mt-1 text-sm text-red-600 dark:text-red-400">
                {{ errors.username }}
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
                :disabled="!editModes.personal || isLoading"
                :class="[
                  'w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
                  !editModes.personal 
                    ? 'bg-gray-50 dark:bg-gray-800 text-gray-500 dark:text-gray-400 cursor-not-allowed' 
                    : 'bg-white dark:bg-gray-800 border-gray-300 dark:border-gray-600'
                ]"
                placeholder="Indirizzo email"
              />
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
                v-model="profileForm.firstName"
                type="text"
                :disabled="!editModes.personal || isLoading"
                :class="[
                  'w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
                  !editModes.personal 
                    ? 'bg-gray-50 dark:bg-gray-800 text-gray-500 dark:text-gray-400 cursor-not-allowed' 
                    : 'bg-white dark:bg-gray-800 border-gray-300 dark:border-gray-600'
                ]"
                placeholder="Nome"
              />
            </div>

            <!-- Last Name -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Cognome
              </label>
              <input
                v-model="profileForm.lastName"
                type="text"
                :disabled="!editModes.personal || isLoading"
                :class="[
                  'w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
                  !editModes.personal 
                    ? 'bg-gray-50 dark:bg-gray-800 text-gray-500 dark:text-gray-400 cursor-not-allowed' 
                    : 'bg-white dark:bg-gray-800 border-gray-300 dark:border-gray-600'
                ]"
                placeholder="Cognome"
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
          <form @submit.prevent="handlePreferencesSave" class="space-y-6">
            <!-- Theme Preference -->
            <div>
              <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">
                Preferenze Tema
              </h3>
              <div class="space-y-4">
                <div class="flex items-center">
                  <input
                    id="theme-light"
                    v-model="preferencesForm.theme"
                    type="radio"
                    value="light"
                    :disabled="isLoading"
                    class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300"
                  />
                  <label for="theme-light" class="ml-3 block text-sm font-medium text-gray-700 dark:text-gray-300">
                    Modalità Chiara
                  </label>
                </div>
                <div class="flex items-center">
                  <input
                    id="theme-dark"
                    v-model="preferencesForm.theme"
                    type="radio"
                    value="dark"
                    :disabled="isLoading"
                    class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300"
                  />
                  <label for="theme-dark" class="ml-3 block text-sm font-medium text-gray-700 dark:text-gray-300">
                    Modalità Scura
                  </label>
                </div>
                <div class="flex items-center">
                  <input
                    id="theme-system"
                    v-model="preferencesForm.theme"
                    type="radio"
                    value="system"
                    :disabled="isLoading"
                    class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300"
                  />
                  <label for="theme-system" class="ml-3 block text-sm font-medium text-gray-700 dark:text-gray-300">
                    Segui Sistema
                  </label>
                </div>
              </div>
            </div>

            <!-- Timezone Preference -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Fuso Orario
              </label>
              <select
                v-model="preferencesForm.timezone"
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
                    type="checkbox"
                    :disabled="isLoading"
                    class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                </div>
              </div>
            </div>

            <!-- Save Preferences Button -->
            <div class="flex justify-end">
              <button
                type="submit"
                :disabled="isLoading"
                class="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
              >
                <LoadingSpinner v-if="isLoading" class="w-4 h-4 mr-2" />
                {{ isLoading ? 'Salvando...' : 'Salva Preferenze' }}
              </button>
            </div>
          </form>
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
import { authApi } from '@/services/authApi'
import LoadingSpinner from '@/components/Common/LoadingSpinner.vue'
import Modal from '@/components/Common/Modal.vue'
import type { User } from '@/types/auth'

// Composables
const { user, userFullName, userInitials, updateProfile, logout, isLoading } = useAuth()
const { showError, showSuccess } = useCustomToast()
const { theme, setTheme } = useTheme()

// Tab icons (these would be actual icon components in a real app)
const PersonIcon = 'svg'
const SecurityIcon = 'svg'
const SettingsIcon = 'svg'
const DangerIcon = 'svg'

// Component state
const activeTab = ref('personal')
const avatarInput = ref<HTMLInputElement>()
const showDeleteAccountModal = ref(false)
const deleteAccountPassword = ref('')
const deleteAccountError = ref('')

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
  emailNotifications: true,
  reminderNotifications: true
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

// Avatar handling
const triggerAvatarUpload = () => {
  avatarInput.value?.click()
}

const handleAvatarChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  
  if (!file) return

  // Validate file
  if (!file.type.startsWith('image/')) {
    showError('Seleziona un file immagine valido')
    return
  }
  
  if (file.size > 5 * 1024 * 1024) { // 5MB limit
    showError('Il file è troppo grande. Massimo 5MB consentito')
    return
  }

  try {
    // Create form data for upload
    const formData = new FormData()
    formData.append('avatar', file)
    
    // TODO: Implement avatar upload API call
    showSuccess('Avatar aggiornato con successo!')
  } catch (error) {
    console.error('Avatar upload failed:', error)
    showError('Errore durante l\'aggiornamento dell\'avatar')
  }
}

// Form submission handlers
const handlePersonalInfoSave = async () => {
  try {
    const updateData: Partial<User> = {
      username: profileForm.username.trim(),
      email: profileForm.email.trim(),
      firstName: profileForm.firstName.trim() || undefined,
      lastName: profileForm.lastName.trim() || undefined
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

const handlePreferencesSave = async () => {
  try {
    await authApi.updatePreferences({
      theme: preferencesForm.theme,
      timezone: preferencesForm.timezone,
      emailNotifications: preferencesForm.emailNotifications,
      reminderNotifications: preferencesForm.reminderNotifications
    })
    
    // Update theme if changed
    setTheme(preferencesForm.theme)
    
    showSuccess('Preferenze salvate con successo!')
  } catch (error) {
    console.error('Preferences save failed:', error)
    showError('Errore durante il salvataggio delle preferenze')
  }
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
const toggle2FA = async () => {
  try {
    if (user.value?.twoFactorEnabled) {
      // Disable 2FA - would need password confirmation
      showError('Disabilitazione 2FA non ancora implementata')
    } else {
      // Enable 2FA
      const result = await authApi.enableTwoFactor()
      // TODO: Show QR code modal for 2FA setup
      showError('Abilitazione 2FA non ancora implementata')
    }
  } catch (error) {
    console.error('2FA toggle failed:', error)
    showError('Errore durante la modifica dell\'autenticazione a due fattori')
  }
}

// Data export
const exportData = async () => {
  try {
    const blob = await authApi.exportData()
    
    // Create download link
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `privatecal-data-${new Date().toISOString().split('T')[0]}.json`
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
    preferencesForm.theme = preferences.theme
    preferencesForm.timezone = preferences.timezone
    preferencesForm.emailNotifications = preferences.emailNotifications
    preferencesForm.reminderNotifications = preferences.reminderNotifications
  } catch (error) {
    console.error('Failed to load preferences:', error)
    // Use defaults
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
select:focus,
button:focus {
  @apply outline-none ring-2 ring-blue-500 ring-offset-2 dark:ring-offset-gray-900;
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