<template>
  <div class="h-screen bg-gray-50 dark:bg-gray-900">
    <!-- Header -->
    <header class="bg-white dark:bg-gray-800 shadow-sm border-b border-gray-200 dark:border-gray-700">
      <div class="px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center h-16">
          <!-- Mobile Menu Button + Logo and Title -->
          <div class="flex items-center">
            <!-- Mobile Sidebar Toggle -->
            <button @click="showMobileSidebar = !showMobileSidebar"
              class="md:hidden p-2 rounded-md text-gray-400 hover:text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700 mr-2"
              title="Toggle menu">
              <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
              </svg>
            </button>

            <h1 class="text-xl md:text-2xl font-bold text-gray-900 dark:text-white">
              PrivateCal
            </h1>
            <span class="ml-2 text-xs md:text-sm text-gray-500 dark:text-gray-400 hidden sm:inline">
              {{ formatDisplayDate(currentDate) }}
            </span>
          </div>

          <!-- Navigation Controls -->
          <div class="flex items-center space-x-2 md:space-x-4">
            <!-- View Mode Selector -->
            <div class="hidden sm:flex bg-gray-100 dark:bg-gray-700 rounded-md p-1">
              <button v-for="view in CALENDAR_VIEWS" :key="view.value" @click="setViewMode(view.value)"
                class="px-2 md:px-3 py-1 text-xs md:text-sm font-medium rounded transition-colors" :class="{
                  'bg-white dark:bg-gray-600 text-gray-900 dark:text-white shadow-sm': viewMode === view.value,
                  'text-gray-700 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white': viewMode !== view.value
                }" :title="`${view.label} (${view.shortcut})`">
                {{ view.label }}
              </button>
            </div>

            <!-- Mobile View Mode Dropdown -->
            <div class="sm:hidden relative">
              <select :value="viewMode" @change="setViewMode($event.target.value)"
                class="bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 px-2 py-1 text-sm rounded-md border-0 focus:ring-2 focus:ring-blue-500">
                <option v-for="view in CALENDAR_VIEWS" :key="view.value" :value="view.value">
                  {{ view.label }}
                </option>
              </select>
            </div>

            <!-- Navigation Buttons -->
            <div class="flex items-center space-x-1">
              <button @click="navigatePrevious"
                class="p-2 text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md"
                title="Periodo precedente (Ctrl + ←)">
                <ChevronLeftIcon class="h-5 w-5" />
              </button>

              <button @click="goToToday"
                class="px-2 md:px-3 py-2 text-xs md:text-sm font-medium text-gray-700 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md"
                title="Vai a oggi (Ctrl + T)">
                <span class="hidden sm:inline">Oggi</span>
                <span class="sm:hidden">•</span>
              </button>

              <button @click="navigateNext"
                class="p-2 text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md"
                title="Periodo successivo (Ctrl + →)">
                <ChevronRightIcon class="h-5 w-5" />
              </button>
            </div>

          </div>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <div class="flex h-[calc(100vh-4rem)] relative">
      <!-- Mobile Sidebar Overlay -->
      <div v-if="showMobileSidebar" @click="showMobileSidebar = false"
        class="fixed inset-0 bg-gray-600 bg-opacity-75 z-40 md:hidden"></div>

      <!-- Sidebar -->
      <aside :class="[
        'bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 overflow-y-auto transition-transform duration-300 ease-in-out z-50',
        'md:relative md:translate-x-0 md:w-80',
        'fixed inset-y-0 left-0 w-80 transform',
        showMobileSidebar ? 'translate-x-0' : '-translate-x-full md:translate-x-0'
      ]">
        <div class="p-4">
          <!-- User Profile & Settings -->
          <div class="mb-6">
            <!-- User Profile Header (Always Visible, Clickable) -->
            <div class="flex items-center justify-between mb-4 cursor-pointer" @click="toggleUserMenu">
              <div class="flex items-center space-x-3">
                <div
                  class="w-10 h-10 bg-blue-600 text-white rounded-full flex items-center justify-center text-sm font-medium">
                  {{ userInitials }}
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-medium text-gray-900 dark:text-white truncate">
                    {{ userFullName }}
                  </p>
                  <p class="text-xs text-gray-500 dark:text-gray-400">
                    {{ user?.username }}
                  </p>
                </div>
              </div>

              <div class="flex items-center space-x-2">
                <!-- Theme Toggle -->
                <button @click.stop="toggleTheme"
                  class="p-2 text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md transition-colors"
                  :title="`Cambia tema: ${themeName}`">
                  <SunIcon v-if="isDarkMode" class="h-5 w-5" />
                  <MoonIcon v-else class="h-5 w-5" />
                </button>
                
                <!-- Dropdown Arrow -->
                <ChevronDownIcon class="h-4 w-4 text-gray-400 transition-transform duration-200" 
                  :class="{ 'rotate-180': showUserMenu }" />
              </div>
            </div>

            <!-- User Menu Actions (Collapsible) -->
            <div v-show="showUserMenu" class="space-y-1 transition-all duration-200">
              <button @click="showProfile"
                class="w-full flex items-center px-3 py-2 text-sm text-gray-700 dark:text-gray-300 rounded-md hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors">
                <svg class="w-4 h-4 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
                Profilo
              </button>

              <button @click="showSettings"
                class="w-full flex items-center px-3 py-2 text-sm text-gray-700 dark:text-gray-300 rounded-md hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors">
                <svg class="w-4 h-4 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
                Impostazioni
              </button>

              <button @click="handleLogout"
                class="w-full flex items-center px-3 py-2 text-sm text-red-600 dark:text-red-400 rounded-md hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors">
                <svg class="w-4 h-4 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                </svg>
                Disconnetti
              </button>
            </div>
          </div>

          <!-- Statistics Section (Collapsible) -->
          <div class="mb-6">
            <!-- Statistics Header (Clickable) -->
            <div class="flex items-center justify-between mb-3 cursor-pointer" @click="toggleStatistics">
              <h3 class="text-sm font-medium text-gray-900 dark:text-white">
                Statistiche
              </h3>
              <ChevronDownIcon class="h-4 w-4 text-gray-400 transition-transform duration-200" 
                :class="{ 'rotate-180': showStatistics }" />
            </div>
            
            <!-- Statistics Content (Collapsible) -->
            <div v-show="showStatistics" class="transition-all duration-200">
              <div class="grid grid-cols-2 gap-2 sm:gap-3">
                <div class="bg-blue-50 dark:bg-blue-900/20 p-2 sm:p-3 rounded-md">
                  <div class="text-lg sm:text-2xl font-bold text-blue-600 dark:text-blue-400">
                    {{ taskStats?.pending ?? 0 }}
                  </div>
                  <div class="text-xs text-blue-600 dark:text-blue-400">
                    In corso
                  </div>
                </div>
                <div class="bg-green-50 dark:bg-green-900/20 p-2 sm:p-3 rounded-md">
                  <div class="text-lg sm:text-2xl font-bold text-green-600 dark:text-green-400">
                    {{ taskStats?.completed ?? 0 }}
                  </div>
                  <div class="text-xs text-green-600 dark:text-green-400">
                    Completate
                  </div>
                </div>
                <div class="bg-yellow-50 dark:bg-yellow-900/20 p-2 sm:p-3 rounded-md">
                  <div class="text-lg sm:text-2xl font-bold text-yellow-600 dark:text-yellow-400">
                    {{ taskStats?.today ?? 0 }}
                  </div>
                  <div class="text-xs text-yellow-600 dark:text-yellow-400">
                    Oggi
                  </div>
                </div>
                <div class="bg-red-50 dark:bg-red-900/20 p-2 sm:p-3 rounded-md">
                  <div class="text-lg sm:text-2xl font-bold text-red-600 dark:text-red-400">
                    {{ taskStats?.overdue ?? 0 }}
                  </div>
                  <div class="text-xs text-red-600 dark:text-red-400">
                    In ritardo
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Today's Tasks -->
          <div class="mb-6" v-if="todayTasks && todayTasks.length > 0">
            <h3 class="text-sm font-medium text-gray-900 dark:text-white mb-3">
              Attività di oggi
            </h3>
            <div class="space-y-2">
              <div v-for="task in todayTasks.slice(0, 5)" :key="task.id" @click="openTaskModal(task)"
                class="p-3 rounded-md cursor-pointer transition-colors" :class="[
                  task.completed
                    ? 'bg-green-50 dark:bg-green-900/20 hover:bg-green-100 dark:hover:bg-green-900/30'
                    : 'bg-gray-50 dark:bg-gray-700 hover:bg-gray-100 dark:hover:bg-gray-600'
                ]">
                <div class="flex items-center space-x-2">
                  <input type="checkbox" :checked="task.completed" @click.stop="toggleTaskCompletion(task.id)"
                    class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded" />
                  <div class="flex-1 min-w-0">
                    <p class="text-sm font-medium truncate" :class="{
                      'text-gray-900 dark:text-white': !task.completed,
                      'text-gray-500 dark:text-gray-400 line-through': task.completed
                    }">
                      {{ task.title }}
                    </p>
                    <p class="text-xs text-gray-500 dark:text-gray-400 truncate">
                      {{ task.dueDate ? formatTime(task.dueDate) : 'Nessuna ora' }}
                    </p>
                  </div>
                  <div class="w-3 h-3 rounded-full" :style="{ backgroundColor: task.color || '#3788d8' }"></div>
                </div>
              </div>
            </div>
            <div v-if="todayTasks && todayTasks.length > 5" class="mt-2 text-center">
              <button class="text-sm text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300">
                Mostra altre {{ todayTasks.length - 5 }} attività
              </button>
            </div>
          </div>

          <!-- Upcoming Reminders -->
          <div v-if="upcomingReminders && upcomingReminders.length > 0">
            <h3 class="text-sm font-medium text-gray-900 dark:text-white mb-3">
              Promemoria imminenti
            </h3>
            <div class="space-y-2">
              <div v-for="reminder in upcomingReminders.slice(0, 3)" :key="reminder.id"
                class="p-3 bg-yellow-50 dark:bg-yellow-900/20 rounded-md">
                <p class="text-sm font-medium text-yellow-800 dark:text-yellow-200">
                  {{ getTaskById(reminder.taskId)?.title || 'Attività eliminata' }}
                </p>
                <p class="text-xs text-yellow-600 dark:text-yellow-400">
                  {{ formatReminderTimeShort(reminder) }}
                </p>
              </div>
            </div>
          </div>

          <!-- New Task Button -->
          <button @click="openCreateTaskModalWithDate()" class="w-full btn btn-primary mb-6" title="Nuova attività (Ctrl + N)">
            <PlusIcon class="h-4 w-4 mr-2" />
            Nuova Attività
          </button>
          
        </div>
      </aside>

      <!-- Calendar Area -->
      <main class="flex-1 flex flex-col overflow-hidden md:ml-0">
        <!-- Calendar Header -->
        <div class="p-3 md:p-4 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 flex-shrink-0">
          <div class="flex items-center justify-between">
            <div v-if="isAgendaView">
              <h2 class="text-base md:text-lg font-medium text-gray-900 dark:text-white">
                Agenda
              </h2>
              <p class="text-xs md:text-sm text-gray-500 dark:text-gray-400 mt-1">
                Visualizza le attività dei prossimi {{ agendaDays }} giorni
              </p>
            </div>
            <h2 v-else class="text-base md:text-lg font-medium text-gray-900 dark:text-white">
              {{ currentMonthName }}
            </h2>
            <!-- Mobile Close Sidebar Button (only visible when sidebar is open) -->
            <button v-if="showMobileSidebar" @click="showMobileSidebar = false"
              class="md:hidden p-2 rounded-md text-gray-400 hover:text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700">
              <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>

        <!-- Calendar Content -->
        <div class="flex-1 p-2 md:p-4 overflow-auto">
          <!-- Month View -->
          <div v-if="isMonthView" class="calendar-grid-mobile md:calendar-grid">
            <!-- Week Days Header -->
            <div v-for="day in LOCALE_STRINGS.weekdaysShort" :key="day" class="calendar-day-header">
              {{ day }}
            </div>

            <!-- Calendar Days -->
            <div v-for="day in calendarDays" :key="day.date.getTime()" @click="selectDate(day.date)"
              @dblclick="openCreateTaskModalWithDate(day.date)"
              class="calendar-day min-h-20 md:min-h-32 cursor-pointer transition-colors p-1 md:p-2" :class="{
                'bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-400': day.isSelected,
                'bg-yellow-50 dark:bg-yellow-900/20': day.isToday && !day.isSelected,
                'text-gray-400 dark:text-gray-600': !day.isCurrentMonth,
                'hover:bg-gray-100 dark:hover:bg-gray-700': !day.isSelected && !day.isToday
              }">
              <!-- Day Number -->
              <div class="flex justify-between items-center mb-2">
                <span class="text-sm font-medium" :class="{
                  'text-blue-600 dark:text-blue-400': day.isSelected,
                  'text-yellow-700 dark:text-yellow-300': day.isToday && !day.isSelected,
                  'text-gray-900 dark:text-white': day.isCurrentMonth && !day.isToday && !day.isSelected,
                  'text-gray-400 dark:text-gray-600': !day.isCurrentMonth
                }">
                  {{ day.dayOfMonth }}
                </span>
                <div v-if="day.tasks && day.tasks.length > 0" class="text-xs text-gray-500 dark:text-gray-400">
                  {{ day.tasks.length }}
                </div>
              </div>

              <!-- Tasks -->
              <div class="space-y-1">
                <div v-for="task in (day.tasks || []).slice(0, 3)" :key="task.id"
                  @click.stop="openTaskModalForEdit(getTaskById(task.id)!)"
                  class="text-xs p-1 rounded truncate cursor-pointer transition-colors"
                  :class="getTaskDisplayClasses(task)">
                  {{ task.completed ? '✓' : '' }} {{ task.title }}
                </div>
                <div v-if="day.tasks && day.tasks.length > 3" class="text-xs text-gray-500 dark:text-gray-400 p-1">
                  +{{ day.tasks.length - 3 }} altro/i
                </div>
              </div>
            </div>
          </div>

          <!-- Week View -->
          <div v-else-if="isWeekView" class="h-full flex flex-col">
            <!-- Week Header -->
            <div class="grid grid-cols-8 gap-px bg-gray-200 dark:bg-gray-600 mb-4">
              <div class="bg-white dark:bg-gray-800 p-2 text-sm font-medium text-center">
                Ora
              </div>
              <div v-for="day in getWeekDays(currentDate)" :key="day.getTime()"
                class="bg-white dark:bg-gray-800 p-2 text-sm font-medium text-center" :class="{
                  'bg-blue-50 dark:bg-blue-900/20': isToday(day),
                  'text-blue-600 dark:text-blue-400': isToday(day)
                }">
                <div>{{ getWeekDayName(day, true) }}</div>
                <div class="text-lg font-bold">{{ day.getDate() }}</div>
              </div>
            </div>

            <!-- Week Grid -->
            <div class="flex-1 relative overflow-hidden">
              <!-- Scrollable Content -->
              <div ref="weeklyScrollContainer" @scroll="handleWeeklyScroll" class="absolute inset-0 overflow-auto">
                <div class="grid grid-cols-8 gap-px bg-gray-200 dark:bg-gray-600" style="min-height: 1536px;">
                  <!-- Time Column -->
                  <div class="bg-white dark:bg-gray-800">
                    <div v-for="hour in 24" :key="hour-1" 
                      class="h-16 border-b border-gray-200 dark:border-gray-600 flex items-center justify-center text-xs font-medium text-gray-500 dark:text-gray-400"
                      :class="{ 'border-b-2 border-gray-300 dark:border-gray-500': (hour-1) % 6 === 0 }">
                      {{ String(hour-1).padStart(2, '0') }}:00
                    </div>
                  </div>

                  <!-- Day Columns -->
                  <div v-for="(dayInfo, dayIndex) in weekDaysWithIndicators" :key="dayInfo.day.getTime()" 
                    class="bg-white dark:bg-gray-800 relative">
                    
                    <!-- Time Grid Lines -->
                    <div v-for="hour in 24" :key="hour-1" 
                      class="h-16 border-b border-gray-100 dark:border-gray-700"
                      :class="{ 
                        'border-b-2 border-gray-200 dark:border-gray-600': (hour-1) % 6 === 0,
                        'bg-blue-50 dark:bg-blue-900/10': isToday(dayInfo.day)
                      }">
                    </div>

                    <!-- Tasks for this day (including split multi-day tasks) -->
                    <div class="absolute inset-0 pointer-events-none">
                      <div v-for="task in getTasksWithSplitsForDate(dayInfo.day)" :key="`${task.id}-${task._splitIndex || 0}`"
                        :style="getTaskTimeStyle(task)"
                        @click="openTaskModalForEdit(task)"
                        class="absolute left-1 right-1 p-1 rounded text-xs font-medium cursor-pointer pointer-events-auto transition-all hover:shadow-md"
                        :class="getTaskTimeDisplayClasses(task)">
                        <div class="truncate font-semibold">{{ task.title }}</div>
                        <div v-if="task.location" class="truncate text-xs opacity-90">{{ task.location }}</div>
                        <div class="text-xs opacity-75">{{ formatTaskTime(task) }}</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- Fixed Overflow Indicators (Positioned relative to scroll container) -->
              <div class="absolute inset-0 pointer-events-none z-50">
                <div v-for="(dayInfo, dayIndex) in weekDaysWithIndicators" :key="`indicators-${dayInfo.day.getTime()}`"
                  class="absolute pointer-events-none"
                  :style="{ 
                    left: `${12.5 + (dayIndex * 12.5)}%`,
                    width: '12.5%',
                    top: '0px',
                    height: '100%'
                  }">
                  
                  <!-- DEBUG: Always show data -->
                  <div class="absolute top-2 left-2 bg-black text-white text-xs px-1 rounded pointer-events-auto">
                    T:{{ dayInfo.indicators.top.length }} B:{{ dayInfo.indicators.bottom.length }}
                  </div>
                  
                  <!-- Top indicators for hidden tasks above -->
                  <div v-for="(task, index) in dayInfo.indicators.top" 
                    :key="`top-${task.id}`"
                    class="absolute w-3 h-3 rounded-full border border-white shadow-sm"
                    :style="{ 
                      top: '16px',
                      right: `${6 + index * 10}px`,
                      backgroundColor: task.color || '#3B82F6'
                    }">
                  </div>

                  <!-- Bottom indicators for hidden tasks below -->
                  <div v-for="(task, index) in dayInfo.indicators.bottom" 
                    :key="`bottom-${task.id}`"
                    class="absolute w-3 h-3 rounded-full border border-white shadow-lg"
                    :style="{ 
                      bottom: '16px',
                      right: `${6 + index * 10}px`,
                      backgroundColor: task.color || '#EF4444'
                    }"
                    :title="`Hidden below: ${task.title}`">
                  </div>
                  
                  <!-- DEBUG: Show count -->
                  <div v-if="dayInfo.indicators.bottom.length > 0" 
                    class="absolute bottom-0 left-0 bg-red-500 text-white text-xs px-1 rounded">
                    ↓{{dayInfo.indicators.bottom.length}}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Day View -->
          <div v-else-if="isDayView" class="h-full">
            <div class="bg-white dark:bg-gray-800 rounded-lg p-6">
              <h3 class="text-xl font-medium text-gray-900 dark:text-white mb-4">
                {{ formatDate(currentDate) }}
              </h3>

              <div class="space-y-3">
                <div v-for="task in getTasksForDate(currentDate)" :key="task.id" @click="openTaskModalForEdit(task)"
                  class="p-4 rounded-lg cursor-pointer transition-colors" :class="getTaskDisplayClasses(task, true)">
                  <div class="flex items-center space-x-3">
                    <input type="checkbox" :checked="task.completed" @click.stop="toggleTaskCompletion(task.id)"
                      class="h-5 w-5 text-blue-600 focus:ring-blue-500 border-gray-300 rounded" />
                    <div class="flex-1">
                      <h4 class="font-medium text-gray-900 dark:text-white">
                        {{ task.title }}
                      </h4>
                      <p v-if="task.description" class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                        {{ task.description }}
                      </p>
                      <div class="flex items-center space-x-4 mt-2 text-sm text-gray-500 dark:text-gray-400">
                        <span>{{ formatTime(task.startDatetime) }} - {{ formatTime(task.endDatetime) }}</span>
                        <span>{{ formatTime(task.startDatetime) }}</span>
                        <span v-if="task.reminders && task.reminders.length > 0">
                          {{ task.reminders.length }} promemoria
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                <div v-if="getTasksForDate(currentDate).length === 0" class="text-center py-8">
                  <p class="text-gray-500 dark:text-gray-400">
                    Nessuna attività programmata per oggi
                  </p>
                  <button @click="openCreateTaskModalWithDate(currentDate)"
                    class="mt-2 text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300">
                    Crea la prima attività
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Agenda View -->
          <div v-else-if="isAgendaView" class="space-y-4">
            <div v-for="(dayTasks, date) in sortedTasksByDateInRange" :key="date"
              class="bg-white dark:bg-gray-800 rounded-lg p-4">
              <h3 class="font-medium text-gray-900 dark:text-white mb-3">
                {{ getDateDescription(new Date(date)) }}
              </h3>
              <div class="space-y-2">
                <div v-for="task in dayTasks" :key="task.id" @click="openTaskModalForEdit(task)"
                  class="p-3 rounded-lg cursor-pointer transition-colors" :class="getTaskDisplayClasses(task)">
                  <div class="flex items-center space-x-3">
                    <input type="checkbox" :checked="task.completed" @click.stop="toggleTaskCompletion(task.id)"
                      class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded" />
                    <div class="flex-1">
                      <p class="font-medium text-gray-900 dark:text-white">
                        {{ task.title }}
                      </p>
                      <div class="flex items-center space-x-2 text-xs text-gray-500 dark:text-gray-400">
                        <span>{{ formatTime(task.startDatetime) }} - {{ formatTime(task.endDatetime) }}</span>
                        <span>{{ formatTime(task.startDatetime) }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="Object.keys(tasksByDateInRange).length === 0" class="text-center py-8">
              <p class="text-gray-500 dark:text-gray-400">
                Nessuna attività in questo periodo
              </p>
            </div>
          </div>
        </div>
      </main>
    </div>

    <!-- Task Modal (placeholder) -->
    <div v-if="showTaskModal" class="modal-overlay" @click="closeTaskModal">
      <div class="modal-content" @click.stop>
        <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-4">
          Dettagli Attività
        </h3>
        <div v-if="selectedTask">
          <p><strong>Titolo:</strong> {{ selectedTask.title }}</p>
          <p v-if="selectedTask.description"><strong>Descrizione:</strong> {{ selectedTask.description }}</p>
          <p><strong>Orario:</strong> {{ formatTime(selectedTask.startDatetime) }} - {{ formatTime(selectedTask.endDatetime) }}</p>
          <p v-if="selectedTask.location"><strong>Luogo:</strong> {{ selectedTask.location }}</p>
          <p v-if="selectedTask.dueDate"><strong>Scadenza:</strong> {{ formatDateTime(selectedTask.dueDate) }}</p>
          <p><strong>Stato:</strong> {{ selectedTask.completed ? 'Completata' : 'In corso' }}</p>
        </div>
        <div class="mt-6 flex justify-end space-x-2">
          <button @click="closeTaskModal" class="btn btn-secondary">
            Chiudi
          </button>
        </div>
      </div>
    </div>

    <!-- Task Modal -->
    <TaskModal
      :show="showCreateTaskModal"
      :task="selectedTaskForEdit"
      :initial-date="createTaskDate"
      @close="handleTaskModalClose"
      @task-created="handleTaskCreated"
      @task-updated="handleTaskUpdated"
      @task-deleted="handleTaskDeleted"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import {
  ChevronLeftIcon,
  ChevronRightIcon,
  ChevronDownIcon,
  PlusIcon,
  SunIcon,
  MoonIcon
} from '@heroicons/vue/24/outline'
import TaskModal from '../components/TaskModal.vue'
import type { Task } from '../types/task'

// Composables
import { useAuth } from '../composables/useAuth'
import { useCalendar } from '../composables/useCalendar'
import { useTasks } from '../composables/useTasks'
import { useReminders } from '../composables/useReminders'
import { useTheme } from '../composables/useTheme'
import { useNotifications } from '../composables/useNotifications'

// Utilities
import {
  formatDate,
  formatDateTime,
  formatTime,
  getDayName,
  getDateDescription,
  isToday as isDateToday
} from '../utils/dateHelpers'
import { it } from 'date-fns/locale'
// import { formatTaskPriority } from '../utils/formatters' // Removed as priority no longer exists
import { CALENDAR_VIEWS, LOCALE_STRINGS } from '../utils/constants'

// Composable instances
const auth = useAuth()
const calendar = useCalendar()
const tasks = useTasks()
const reminders = useReminders()
const theme = useTheme()
const { showError, showConfirmation } = useNotifications()

// Reactive state
const showMobileSidebar = ref(false)
const showUserMenu = ref(false)
const showStatistics = ref(false)
const selectedTaskForEdit = ref<Task | null>(null)
const createTaskDate = ref<Date | undefined>(undefined)

// Weekly view scroll tracking
const weeklyScrollContainer = ref<HTMLElement | null>(null)
const scrollTop = ref(0)
const scrollHeight = ref(0)
const clientHeight = ref(0)

// Force reactivity with ref
const indicatorsUpdateTrigger = ref(0)

// Watch scroll changes to force indicators update
watch([scrollTop, clientHeight, scrollHeight], () => {
  indicatorsUpdateTrigger.value++
})

// Computed property for overflow indicators to ensure reactivity
const weekDaysWithIndicators = computed(() => {
  // Force reactivity on scroll changes
  indicatorsUpdateTrigger.value
  
  const weekDays = calendar.getWeekDays(currentDate.value)
  return weekDays.map(day => ({
    day,
    indicators: getTasksOverflowIndicators(calendar.getTasksForDate(day))
  }))
})

// Computed properties from composables
const {
  user,
  userFullName,
  userInitials,
  logout
} = auth

const {
  currentDate,
  selectedDate,
  viewMode,
  agendaDays,
  currentMonthName,
  calendarDays,
  todayTasks,
  isMonthView,
  isWeekView,
  isDayView,
  isAgendaView,
  showTaskModal,
  selectedTask,
  showCreateTaskModal,
  setViewMode,
  navigatePrevious,
  navigateNext,
  goToToday,
  selectDate,
  openTaskModal,
  closeTaskModal,
  openCreateTaskModal,
  closeCreateTaskModal,
  formatDisplayDate,
  getTasksForDate,
  getWeekDays
} = calendar

const {
  taskStats,
  getTaskById,
  toggleTaskCompletion
} = tasks

const {
  upcomingReminders,
  formatReminderTimeShort
} = reminders

const {
  isDarkMode,
  themeName,
  toggleTheme
} = theme

// Additional computed properties
const tasksByDateInRange = computed(() => {
  const range = calendar.viewDateRange.value
  if (!range) return {}

  const result: Record<string, any[]> = {}
  const allTasks = tasks.allTasks.value || []

  console.log('📅 Agenda view debug:', {
    range,
    totalTasks: allTasks.length,
    tasks: allTasks.map(t => ({ id: t.id, title: t.title, startDatetime: t.startDatetime }))
  })

  allTasks.forEach(task => {
    if (task && task.startDatetime) {
      const taskDate = new Date(task.startDatetime)
      if (taskDate >= range.start && taskDate <= range.end) {
        const dateKey = formatDate(taskDate, 'yyyy-MM-dd')
        if (!result[dateKey]) result[dateKey] = []
        result[dateKey].push(task)
        console.log(`✅ Task "${task.title}" added to agenda for ${dateKey}`)
      } else {
        console.log(`❌ Task "${task.title}" outside range:`, {
          taskDate: taskDate.toISOString(),
          rangeStart: range.start.toISOString(),
          rangeEnd: range.end.toISOString()
        })
      }
    } else {
      console.log(`⚠️ Task missing startDatetime:`, task)
    }
  })

  console.log('📅 Final agenda result:', result)
  return result
})

// Sort tasks by date for agenda view
const sortedTasksByDateInRange = computed(() => {
  const tasks = tasksByDateInRange.value
  
  // Convert object to array of [date, tasks] pairs and sort by date
  const sortedEntries = Object.entries(tasks)
    .sort(([dateA], [dateB]) => {
      return new Date(dateA).getTime() - new Date(dateB).getTime()
    })
    .map(([date, dayTasks]) => [
      date, 
      // Also sort tasks within each day by start time
      dayTasks.sort((a, b) => {
        return new Date(a.startDatetime).getTime() - new Date(b.startDatetime).getTime()
      })
    ])

  // Convert back to object
  const sortedResult: Record<string, any[]> = {}
  sortedEntries.forEach(([date, dayTasks]) => {
    sortedResult[date] = dayTasks
  })
  
  console.log('📅 Sorted agenda result:', sortedResult)
  return sortedResult
})

// Methods
const isToday = (date: Date) => isDateToday(date)

// getPriorityColor removed as priority no longer exists in Task model

const getTaskDisplayClasses = (task: any, detailed = false) => {
  const baseClasses = detailed
    ? 'border-l-4'
    : ''

  if (task.completed) {
    return `${baseClasses} bg-green-50 dark:bg-green-900/20 border-green-500 hover:bg-green-100 dark:hover:bg-green-900/30`
  }

  if (task.isOverdue) {
    return `${baseClasses} bg-red-50 dark:bg-red-900/20 border-red-500 hover:bg-red-100 dark:hover:bg-red-900/30`
  }

  // Use task color for styling
  const color = task.color || '#3788d8'
  const colorMap: Record<string, string> = {
    '#3b82f6': 'blue',
    '#10b981': 'emerald', 
    '#ef4444': 'red',
    '#f59e0b': 'amber',
    '#8b5cf6': 'violet',
    '#ec4899': 'pink',
    '#6366f1': 'indigo',
    '#14b8a6': 'teal',
    '#f97316': 'orange',
    '#6b7280': 'gray'
  }
  
  const colorName = colorMap[color] || 'blue'
  return `${baseClasses} bg-${colorName}-50 dark:bg-${colorName}-900/20 border-${colorName}-500 hover:bg-${colorName}-100 dark:hover:bg-${colorName}-900/30`
}

const showProfile = () => {
  showError('Funzionalità profilo non ancora implementata')
  showMobileSidebar.value = false
}

const showSettings = () => {
  showError('Funzionalità impostazioni non ancora implementata')
  showMobileSidebar.value = false
}

const handleLogout = () => {
  showMobileSidebar.value = false
  showConfirmation(
    'Sei sicuro di voler uscire?',
    async () => {
      await logout()
    }
  )
}

const handleKeyboardShortcuts = (event: KeyboardEvent) => {
  calendar.handleKeyboardNavigation(event)

  // Additional shortcuts
  if (event.ctrlKey && event.key === 'n') {
    event.preventDefault()
    openCreateTaskModalWithDate()
  }
}

const handleClickOutside = (event: Event) => {
  // Handle click outside for mobile sidebar if needed
}

const closeMobileSidebar = () => {
  showMobileSidebar.value = false
}

// Close mobile sidebar on route/navigation changes
const handleMobileSidebarClose = () => {
  if (showMobileSidebar.value) {
    showMobileSidebar.value = false
  }
}

// Accordion Methods
const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value
  if (showUserMenu.value) {
    showStatistics.value = false
  }
}

const toggleStatistics = () => {
  showStatistics.value = !showStatistics.value
  if (showStatistics.value) {
    showUserMenu.value = false
  }
}

// Task Modal Methods
const handleTaskModalClose = () => {
  selectedTaskForEdit.value = null
  createTaskDate.value = undefined
  closeCreateTaskModal()
}

const handleTaskCreated = async (task: Task) => {
  // Refresh tasks data
  await tasks.fetchTasks()
  await tasks.refreshStatistics()
}

const handleTaskUpdated = async (task: Task) => {
  // Refresh tasks data
  await tasks.fetchTasks()
  await tasks.refreshStatistics()
}

const handleTaskDeleted = async (taskId: number) => {
  // Refresh tasks data
  await tasks.fetchTasks()
  await tasks.refreshStatistics()
}

// Override calendar methods to use our enhanced modal
const openCreateTaskModalWithDate = (date?: Date) => {
  selectedTaskForEdit.value = null
  createTaskDate.value = date
  calendar.openCreateTaskModal(date)
}

const openTaskModalForEdit = (task: Task) => {
  selectedTaskForEdit.value = task
  createTaskDate.value = undefined
  calendar.closeTaskModal()
  calendar.openCreateTaskModal()
}

// Weekly view scroll management
const handleWeeklyScroll = (event: Event) => {
  const target = event.target as HTMLElement
  scrollTop.value = target.scrollTop
  scrollHeight.value = target.scrollHeight
  clientHeight.value = target.clientHeight
}

// Overflow indicators logic
const getTasksOverflowIndicators = (dayTasks: Task[]) => {
  if (!dayTasks.length) return { top: [], bottom: [] }
  
  const visibleTop = scrollTop.value
  const visibleBottom = scrollTop.value + clientHeight.value
  
  const topHiddenTasks: Task[] = []
  const bottomHiddenTasks: Task[] = []
  
  console.log('📊 Indicator calculation:', {
    visibleTop,
    visibleBottom,
    scrollTop: scrollTop.value,
    clientHeight: clientHeight.value,
    tasksCount: dayTasks.length
  })
  
  dayTasks.forEach(task => {
    if (!task.startDatetime) return
    
    const start = new Date(task.startDatetime)
    const end = task.endDatetime ? new Date(task.endDatetime) : new Date(start.getTime() + 60 * 60 * 1000)
    
    const startHour = start.getHours() + start.getMinutes() / 60
    let endHour = end.getHours() + end.getMinutes() / 60
    
    // Fix for tasks that span midnight or have end time before start time
    if (endHour < startHour) {
      endHour += 24 // Add 24 hours if end is next day
    }
    
    const taskTop = startHour * 64
    const taskBottom = endHour * 64
    
    console.log(`📋 Task "${task.title}":`, {
      startHour,
      endHour,
      taskTop,
      taskBottom,
      visibleTop,
      visibleBottom,
      isHiddenAbove: taskBottom <= visibleTop,
      isHiddenBelow: taskTop >= visibleBottom,
      isVisible: !(taskBottom <= visibleTop) && !(taskTop >= visibleBottom)
    })
    
    // Task completamente sopra l'area visibile
    if (taskBottom <= visibleTop) {
      topHiddenTasks.push(task)
      console.log(`⬆️ Adding "${task.title}" to TOP indicators`)
    }
    // Task completamente sotto l'area visibile  
    else if (taskTop >= visibleBottom) {
      bottomHiddenTasks.push(task)
      console.log(`⬇️ Adding "${task.title}" to BOTTOM indicators`)
    } else {
      console.log(`👁️ "${task.title}" is VISIBLE`)
    }
  })
  
  console.log('🎯 Final result:', {
    topCount: topHiddenTasks.length,
    bottomCount: bottomHiddenTasks.length,
    topTasks: topHiddenTasks.map(t => t.title),
    bottomTasks: bottomHiddenTasks.map(t => t.title)
  })
  
  return {
    top: topHiddenTasks,
    bottom: bottomHiddenTasks
  }
}

// Weekly view helper methods
const getTaskTimePosition = (task: any) => {
  // Use visual times for positioning if available (for split multi-day tasks)
  const startTimeStr = task._visualStartTime || task.startDatetime
  const endTimeStr = task._visualEndTime || task.endDatetime
  
  if (!startTimeStr) return { top: '0px', height: '32px' }
  
  const start = new Date(startTimeStr)
  const end = endTimeStr ? new Date(endTimeStr) : new Date(start.getTime() + 60 * 60 * 1000) // Default 1 hour
  
  // Calculate position based on hours (each hour = 64px height)
  // All times now use local timezone for consistent positioning
  const startHour = start.getHours() + start.getMinutes() / 60
  let endHour = end.getHours() + end.getMinutes() / 60
  
  // For VISUAL positioning: limit to current day (max 24:00)
  if (endHour < startHour) {
    // Task spans midnight - show only until end of day for this column
    endHour = 24
  }
  
  const topPosition = startHour * 64 // 64px per hour (h-16 = 4rem = 64px)
  const height = Math.max((endHour - startHour) * 64, 32) // Minimum 32px height
  
  return {
    top: `${topPosition}px`,
    height: `${height}px`
  }
}

const getTaskTimeDisplayClasses = (task: Task) => {
  const baseClasses = ['border-l-4']
  
  // Color based on task color or default
  if (task.color) {
    const style = document.createElement('div')
    style.style.backgroundColor = task.color
    const rgb = window.getComputedStyle(style).backgroundColor
    baseClasses.push('text-white')
    // We'll use inline styles for custom colors
  } else {
    // Default blue theme
    baseClasses.push('bg-blue-500 text-white border-blue-700')
  }
  
  if (task.completed) {
    baseClasses.push('opacity-60 line-through')
  }
  
  return baseClasses.join(' ')
}

const getTaskTimeStyle = (task: Task) => {
  const positionStyle = getTaskTimePosition(task)
  
  if (task.color) {
    return {
      ...positionStyle,
      backgroundColor: task.color,
      borderLeftColor: task.color
    }
  }
  
  return positionStyle
}

const formatTaskTime = (task: Task) => {
  if (!task.startDatetime) return ''
  
  const start = new Date(task.startDatetime)
  const startTime = formatDate(start, 'HH:mm')
  
  if (task.endDatetime) {
    const end = new Date(task.endDatetime)
    const endTime = formatDate(end, 'HH:mm')
    return `${startTime} - ${endTime}`
  }
  
  return startTime
}

const getWeekDayName = (date: Date, short = false) => {
  return formatDate(date, short ? 'EEE' : 'EEEE', { locale: it })
}

// Lifecycle
onMounted(async () => {
  // Initialize auth and require authentication
  await auth.requireAuth()

  // Initialize calendar view mode
  calendar.initializeViewMode()

  // Fetch initial data
  await tasks.fetchTasks()
  await reminders.fetchAllReminders()

  // Fetch accurate statistics from dedicated endpoints
  await tasks.refreshStatistics()

  // Set up event listeners
  document.addEventListener('keydown', handleKeyboardShortcuts)
  document.addEventListener('click', handleClickOutside)

  // Initialize scroll values for weekly view
  if (weeklyScrollContainer.value) {
    scrollTop.value = weeklyScrollContainer.value.scrollTop
    scrollHeight.value = weeklyScrollContainer.value.scrollHeight
    clientHeight.value = weeklyScrollContainer.value.clientHeight
  }
})

// Multi-day task splitting logic - collect all tasks from the week first
const allWeekTasks = computed(() => {
  const weekDays = calendar.getWeekDays(currentDate.value)
  const allTasks: any[] = []
  
  // Collect all unique tasks from the week
  weekDays.forEach(day => {
    const dayTasks = calendar.getTasksForDate(day)
    dayTasks.forEach(task => {
      if (!allTasks.find(t => t.id === task.id)) {
        allTasks.push(task)
      }
    })
  })
  
  return allTasks
})

const getTasksWithSplitsForDate = (date: Date) => {
  const currentDay = formatDate(date, 'yyyy-MM-dd')
  const allTasksWithSplits: any[] = []
  
  console.log(`🌞 Processing tasks for ${currentDay}`)
  
  // Check ALL week tasks, not just ones assigned to this day
  allWeekTasks.value.forEach(task => {
    if (!task.startDatetime) {
      // Task without start time - only show on its "assigned" day
      const dayTasks = calendar.getTasksForDate(date)
      if (dayTasks.find(t => t.id === task.id)) {
        allTasksWithSplits.push(task)
      }
      return
    }
    
    const startDate = new Date(task.startDatetime)
    const endDate = task.endDatetime ? new Date(task.endDatetime) : startDate
    const taskStartDay = formatDate(startDate, 'yyyy-MM-dd')
    const taskEndDay = formatDate(endDate, 'yyyy-MM-dd')
    
    console.log(`📋 Task "${task.title}":`, {
      taskStartDay,
      taskEndDay,
      currentDay,
      spansMultipleDays: taskStartDay !== taskEndDay
    })
    
    // Check if this task should appear on the current day
    if (currentDay >= taskStartDay && currentDay <= taskEndDay) {
      
      if (taskStartDay === taskEndDay) {
        // Single day task - show as normal but only on the correct day
        if (currentDay === taskStartDay) {
          allTasksWithSplits.push(task)
        }
      } else {
        // Multi-day task - create split version for visual positioning only
        let visualStartTime: string
        let visualEndTime: string
        
        if (currentDay === taskStartDay) {
          // First day: from original start time to end of day
          visualStartTime = task.startDatetime
          visualEndTime = `${currentDay}T23:59:59`
          console.log(`🚀 First day split for "${task.title}": visual ${visualStartTime} → ${visualEndTime}`)
        } else if (currentDay === taskEndDay) {
          // Last day: from start of day to original end time  
          visualStartTime = `${currentDay}T00:00:00`
          visualEndTime = task.endDatetime
          console.log(`🏁 Last day split for "${task.title}": visual ${visualStartTime} → ${visualEndTime}`)
        } else {
          // Middle day: full day (using local timezone format for consistent positioning)
          visualStartTime = `${currentDay}T00:00:00`
          visualEndTime = `${currentDay}T23:59:59`
          console.log(`🔄 Middle day split for "${task.title}": visual full day`)
        }
        
        // Create task with ORIGINAL data but visual positioning times
        const splitTask = {
          ...task, // Keep ALL original data
          // Add visual positioning properties for rendering only
          _visualStartTime: visualStartTime,
          _visualEndTime: visualEndTime,
          _splitIndex: currentDay // For unique key
        }
        
        allTasksWithSplits.push(splitTask)
      }
    }
  })
  
  console.log(`✅ Final tasks for ${currentDay}:`, allTasksWithSplits.map(t => ({
    title: t.title,
    originalStart: t.startDatetime,
    originalEnd: t.endDatetime,
    visualStart: t._visualStartTime || t.startDatetime,
    visualEnd: t._visualEndTime || t.endDatetime
  })))
  
  return allTasksWithSplits
}

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyboardShortcuts)
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
/* Calendar Grid Styles */
.calendar-grid {
  @apply grid grid-cols-7 gap-px bg-gray-200 dark:bg-gray-600 rounded-lg overflow-hidden;
}

.calendar-grid-mobile {
  @apply grid grid-cols-7 gap-px bg-gray-200 dark:bg-gray-600 rounded-lg overflow-hidden;
}

.calendar-day-header {
  @apply bg-gray-50 dark:bg-gray-700 p-2 md:p-3 text-center text-xs md:text-sm font-medium text-gray-700 dark:text-gray-300;
}

.calendar-day {
  @apply bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 flex flex-col;
}

/* Mobile specific adjustments */
@media (max-width: 768px) {
  .calendar-day-header {
    @apply p-2 text-xs;
  }

  .calendar-day {
    @apply text-xs;
  }
}

/* Responsive modal positioning */
.modal-overlay {
  @apply fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50 flex items-center justify-center p-4;
}

.modal-content {
  @apply relative bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto m-4;
  @apply p-4 md:p-6;
}

/* Responsive utility classes */
@media (max-width: 640px) {
  .mobile-hidden {
    @apply hidden;
  }

  .mobile-full {
    @apply w-full;
  }
}
</style>