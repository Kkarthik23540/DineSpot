/**
 * DineSpot Complete Web Engine - Full Android App Pages & Navigation Replica
 */

// Firebase Configuration from project google-services.json
const firebaseConfig = {
  apiKey: "AIzaSyCoj6JbhKbhBlRZeiICqrBKgGHNDWht6Dw",
  authDomain: "dinespot-91d4b.firebaseapp.com",
  databaseURL: "https://dinespot-91d4b-default-rtdb.firebaseio.com",
  projectId: "dinespot-91d4b",
  storageBucket: "dinespot-91d4b.firebasestorage.app",
  messagingSenderId: "1072182836759",
  appId: "1:1072182836759:web:dinespot"
};

// Initialize Firebase SDKs
firebase.initializeApp(firebaseConfig);
const auth = firebase.auth();
const db = firebase.firestore();
const rtdb = firebase.database();

// Global State
let currentUser = null;
let currentRole = 'user'; // 'user' or 'admin'
let activeRestaurant = null;
let cart = [];
let restaurants = [];
let foodItems = [];
let bookings = [];

// Initial Sample Data for Firestore
const sampleRestaurants = [
  {
    name: "La Bella Italia",
    address: "123 Gourmet Street, Downtown",
    category: "Italian",
    imageUrl: "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=600&q=80",
    rating: 4.8,
    noOfTables: 12,
    phoneNumber: "+91 9876543210",
    openingTime: "11:00 AM",
    closingTime: "11:00 PM",
    status: "Open"
  },
  {
    name: "Royal Spice Indian Bistro",
    address: "45 Heritage Avenue, Midtown",
    category: "Indian",
    imageUrl: "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=600&q=80",
    rating: 4.9,
    noOfTables: 15,
    phoneNumber: "+91 9876543211",
    openingTime: "12:00 PM",
    closingTime: "11:30 PM",
    status: "Open"
  },
  {
    name: "Urban Burger & Brew",
    address: "88 Culinary Hub, Westside",
    category: "Fast Food",
    imageUrl: "https://images.unsplash.com/photo-1550547660-d9450f859349?auto=format&fit=crop&w=600&q=80",
    rating: 4.6,
    noOfTables: 10,
    phoneNumber: "+91 9876543212",
    openingTime: "10:00 AM",
    closingTime: "10:00 PM",
    status: "Open"
  }
];

const sampleFoodItems = [
  { name: "Margherita Pizza", price: 399, category: "Main Course", veg: true, imageUrl: "https://images.unsplash.com/photo-1604382354936-07c5d9983bd3?auto=format&fit=crop&w=400&q=80" },
  { name: "Creamy Alfredo Pasta", price: 349, category: "Main Course", veg: true, imageUrl: "https://images.unsplash.com/photo-1621996346565-e3d5d6281270?auto=format&fit=crop&w=400&q=80" },
  { name: "Paneer Tikka Masala", price: 380, category: "Main Course", veg: true, imageUrl: "https://images.unsplash.com/photo-1565557623262-b51c2513a641?auto=format&fit=crop&w=400&q=80" },
  { name: "Butter Chicken", price: 420, category: "Main Course", veg: false, imageUrl: "https://images.unsplash.com/photo-1588166524941-3bf61a9c41db?auto=format&fit=crop&w=400&q=80" },
  { name: "Classic Cheeseburger", price: 249, category: "Starters", veg: false, imageUrl: "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=400&q=80" },
  { name: "Tiramisu Dessert", price: 199, category: "Desserts", veg: true, imageUrl: "https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?auto=format&fit=crop&w=400&q=80" }
];

document.addEventListener('DOMContentLoaded', () => {
  setupEventListeners();
  listenToAuth();
  fetchRestaurants();
  fetchBookings();
});

// Single Page View Router
function navigateTo(viewId) {
  document.querySelectorAll('.page-view').forEach(view => view.classList.remove('active'));
  const target = document.getElementById(viewId);
  if (target) {
    target.classList.add('active');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  // Hide header and bottom nav on Welcome & Login screens
  const header = document.getElementById('mainHeader');
  const filterBar = document.getElementById('mainFilterBar');
  const bottomNav = document.getElementById('mainBottomNav');

  if (viewId === 'viewWelcome' || viewId === 'viewLogin' || viewId === 'viewSignUp' || viewId === 'viewForgotPassword') {
    if (header) header.style.display = 'none';
    if (filterBar) filterBar.style.display = 'none';
    if (bottomNav) bottomNav.style.display = 'none';
  } else {
    if (header) header.style.display = 'block';
    if (filterBar) filterBar.style.display = 'block';
    if (bottomNav) bottomNav.style.display = 'flex';
  }
}

function setupEventListeners() {
  document.getElementById('roleToggleBtn')?.addEventListener('click', toggleRole);
  document.getElementById('searchInput')?.addEventListener('input', handleSearch);
}

function listenToAuth() {
  auth.onAuthStateChanged(user => {
    currentUser = user;
    if (user) {
      document.getElementById('profileEmail').innerText = user.email;
      document.getElementById('profileName').innerText = user.displayName || user.email.split('@')[0];
    } else {
      document.getElementById('profileEmail').innerText = 'guest@dinespot.com';
      document.getElementById('profileName').innerText = 'Guest Diner';
    }
  });
}

function handleLogin(e) {
  e.preventDefault();
  const email = document.getElementById('loginEmail').value;
  const password = document.getElementById('loginPassword').value;

  auth.signInWithEmailAndPassword(email, password)
    .then(() => navigateTo('viewHome'))
    .catch(err => alert("Login Error: " + err.message));
}

function handleSignUp(e) {
  e.preventDefault();
  const name = document.getElementById('signUpName').value;
  const email = document.getElementById('signUpEmail').value;
  const password = document.getElementById('signUpPassword').value;

  auth.createUserWithEmailAndPassword(email, password)
    .then(res => {
      res.user.updateProfile({ displayName: name });
      alert("Account created successfully!");
      navigateTo('viewHome');
    })
    .catch(err => alert("Sign Up Error: " + err.message));
}

function handleForgotPassword(e) {
  e.preventDefault();
  const email = document.getElementById('forgotEmail').value;
  auth.sendPasswordResetEmail(email)
    .then(() => alert("Reset password email sent!"))
    .catch(err => alert("Error: " + err.message));
}

function toggleRole() {
  currentRole = currentRole === 'user' ? 'admin' : 'user';
  const roleBtn = document.getElementById('roleToggleBtn');

  if (currentRole === 'admin') {
    roleBtn.innerHTML = `<i class="fas fa-user-shield"></i> Switch to Diner Portal`;
    document.getElementById('userFilterBar').style.display = 'none';
    document.getElementById('adminFilterBar').style.display = 'flex';
    renderAdminDashboard();
    navigateTo('viewAdminDashboard');
  } else {
    roleBtn.innerHTML = `<i class="fas fa-store"></i> Switch to Admin Portal`;
    document.getElementById('userFilterBar').style.display = 'flex';
    document.getElementById('adminFilterBar').style.display = 'none';
    renderRestaurants(restaurants);
    navigateTo('viewHome');
  }
}

async function fetchRestaurants() {
  try {
    const snapshot = await db.collection('restaurants').get();
    if (snapshot.empty) {
      for (const res of sampleRestaurants) {
        const docRef = await db.collection('restaurants').add(res);
        restaurants.push({ id: docRef.id, ...res });
      }
    } else {
      restaurants = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    }
    renderRestaurants(restaurants);
    renderNearbyRestaurants(restaurants);
    fetchFoodItems();
  } catch (e) {
    restaurants = sampleRestaurants.map((r, i) => ({ id: `res_${i}`, ...r }));
    renderRestaurants(restaurants);
    renderNearbyRestaurants(restaurants);
  }
}

async function fetchFoodItems() {
  try {
    const snapshot = await db.collection('food_items').get();
    if (snapshot.empty && restaurants.length > 0) {
      for (const food of sampleFoodItems) {
        food.restaurantId = restaurants[0].id;
        const docRef = await db.collection('food_items').add(food);
        foodItems.push({ id: docRef.id, ...food });
      }
    } else {
      foodItems = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    }
  } catch (e) {
    foodItems = sampleFoodItems.map((f, i) => ({ id: `food_${i}`, ...f }));
  }
}

async function fetchBookings() {
  try {
    const snapshot = await db.collection('bookings').get();
    bookings = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    renderBookingHistory();
  } catch (e) {
    bookings = [];
  }
}

function renderRestaurants(list) {
  const container = document.getElementById('restaurantGrid');
  if (!container) return;

  container.innerHTML = list.map(r => `
    <div class="card" onclick="openRestaurantDetails('${r.id}')">
      <div class="card-img-wrapper">
        <img src="${r.imageUrl}" class="card-img" alt="${r.name}">
        <span class="status-badge">${r.status || 'Open'}</span>
        <span class="rating-badge"><i class="fas fa-star"></i> ${r.rating || 4.8}</span>
      </div>
      <div class="card-content">
        <div class="card-title">${r.name}</div>
        <div class="card-subtitle"><i class="fas fa-map-marker-alt"></i> ${r.address}</div>
        <div style="font-size: 0.8rem; color: var(--text-muted); margin-bottom: 0.8rem;">
          <i class="far fa-clock"></i> ${r.openingTime || '11:00 AM'} - ${r.closingTime || '11:00 PM'}
        </div>
        <button class="btn btn-outline btn-block">Reserve Table & View Menu</button>
      </div>
    </div>
  `).join('');
}

function renderNearbyRestaurants(list) {
  const container = document.getElementById('nearbyGrid');
  if (!container) return;

  container.innerHTML = list.slice(0, 3).map(r => `
    <div class="card" style="border-radius: 16px;" onclick="openRestaurantDetails('${r.id}')">
      <div class="card-img-wrapper" style="height: 120px;">
        <img src="${r.imageUrl}" class="card-img" alt="${r.name}">
        <span class="rating-badge" style="bottom: 8px; right: 8px;"><i class="fas fa-star"></i> ${r.rating || 4.8}</span>
      </div>
      <div class="card-content" style="padding: 0.8rem;">
        <div class="card-title" style="font-size: 0.95rem;">${r.name}</div>
        <div class="card-subtitle" style="font-size: 0.75rem; margin-bottom: 0;"><i class="fas fa-location-arrow" style="color: var(--primary);"></i> 1.2 km away</div>
      </div>
    </div>
  `).join('');
}

function openRestaurantDetails(resId) {
  activeRestaurant = restaurants.find(r => r.id === resId);
  if (!activeRestaurant) return;

  document.getElementById('detailsResName').innerText = activeRestaurant.name;
  document.getElementById('detailsResAddress').innerText = activeRestaurant.address;
  document.getElementById('detailsResCategory').innerText = activeRestaurant.category || 'Multi-Cuisine';
  document.getElementById('detailsResRating').innerText = activeRestaurant.rating || '4.8';
  document.getElementById('detailsResHours').innerText = `${activeRestaurant.openingTime || '11 AM'} - ${activeRestaurant.closingTime || '11 PM'}`;
  document.getElementById('detailsResPhone').innerText = activeRestaurant.phoneNumber || '+91 9876543210';
  document.getElementById('detailsResBanner').src = activeRestaurant.imageUrl;

  const items = foodItems.filter(f => f.restaurantId === resId || !f.restaurantId);
  const menuList = document.getElementById('detailsMenuList');

  menuList.innerHTML = items.map(item => `
    <div style="display: flex; justify-content: space-between; align-items: center; padding: 0.8rem 0; border-bottom: 1px solid var(--divider);">
      <div style="display: flex; gap: 0.8rem; align-items: center;">
        <img src="${item.imageUrl}" style="width: 60px; height: 60px; border-radius: 12px; object-fit: cover;">
        <div>
          <div style="font-weight: 700; font-size: 0.95rem;">${item.name} ${item.veg ? '🟢' : '🔴'}</div>
          <div style="color: var(--primary); font-weight: 700; font-size: 0.9rem;">₹${item.price}</div>
        </div>
      </div>
      <button class="btn btn-primary" style="padding: 0.4rem 0.9rem; font-size: 0.85rem;" onclick="addToCart('${item.id}')">+ Add</button>
    </div>
  `).join('');

  navigateTo('viewRestaurantDetails');
}

function submitTableBooking() {
  const date = document.getElementById('bookingDate').value;
  const timeSlot = document.getElementById('bookingTimeSlot').value;
  const seating = document.getElementById('bookingSeating').value;
  const requests = document.getElementById('bookingRequests').value;

  if (!date || !timeSlot) {
    alert("Please select a date and time slot for your table reservation.");
    return;
  }

  const newBooking = {
    restaurantName: activeRestaurant ? activeRestaurant.name : "La Bella Italia",
    restaurantId: activeRestaurant ? activeRestaurant.id : "res_0",
    userId: currentUser ? currentUser.uid : "guest_user",
    userName: currentUser ? currentUser.displayName || currentUser.email.split('@')[0] : "Guest Diner",
    phoneNumber: "+91 9876543210",
    dateTime: date,
    timeSlot: timeSlot,
    tableType: seating,
    specialRequests: requests,
    status: "Pending",
    orderedItems: cart.map(i => ({ name: i.name, price: i.price, quantity: i.quantity }))
  };

  db.collection('bookings').add(newBooking).then(docRef => {
    bookings.push({ id: docRef.id, ...newBooking });
    alert("Table Booking Request Submitted Successfully!");
    navigateTo('viewBookingHistory');
  }).catch(() => {
    bookings.push({ id: `book_${Date.now()}`, ...newBooking });
    alert("Table Booking Request Submitted!");
    navigateTo('viewBookingHistory');
  });
}

function addToCart(foodId) {
  const item = foodItems.find(f => f.id === foodId);
  if (!item) return;

  const existing = cart.find(c => c.id === foodId);
  if (existing) {
    existing.quantity += 1;
  } else {
    cart.push({ ...item, quantity: 1 });
  }

  updateCartUI();
  alert(`Added ${item.name} to Cart!`);
}

function updateCartUI() {
  const cartList = document.getElementById('cartItemsList');
  if (!cartList) return;

  if (cart.length === 0) {
    cartList.innerHTML = `<p style="text-align: center; color: var(--text-muted); padding: 2rem;">Your cart is empty.</p>`;
    document.getElementById('cartSubtotal').innerText = "₹0";
    document.getElementById('cartTotal').innerText = "₹0";
    return;
  }

  cartList.innerHTML = cart.map(item => `
    <div style="display: flex; justify-content: space-between; align-items: center; padding: 0.8rem 0; border-bottom: 1px solid var(--divider);">
      <div>
        <div style="font-weight: 700;">${item.name}</div>
        <div style="color: var(--primary); font-weight: 700;">₹${item.price * item.quantity}</div>
      </div>
      <div style="display: flex; align-items: center; gap: 0.5rem;">
        <button class="btn btn-outline" style="padding: 0.2rem 0.6rem;" onclick="changeQuantity('${item.id}', -1)">-</button>
        <span style="font-weight: 700;">${item.quantity}</span>
        <button class="btn btn-outline" style="padding: 0.2rem 0.6rem;" onclick="changeQuantity('${item.id}', 1)">+</button>
      </div>
    </div>
  `).join('');

  const subtotal = cart.reduce((sum, i) => sum + (i.price * i.quantity), 0);
  const total = subtotal + 40;

  document.getElementById('cartSubtotal').innerText = `₹${subtotal}`;
  document.getElementById('cartTotal').innerText = `₹${total}`;
}

function changeQuantity(id, delta) {
  const item = cart.find(c => c.id === id);
  if (!item) return;

  item.quantity += delta;
  if (item.quantity <= 0) {
    cart = cart.filter(c => c.id !== id);
  }
  updateCartUI();
}

function initiateCheckout() {
  if (cart.length === 0) {
    alert("Cart is empty! Please add food items first.");
    return;
  }

  const subtotal = cart.reduce((sum, i) => sum + (i.price * i.quantity), 0);
  const totalAmount = (subtotal + 40) * 100;

  const options = {
    key: "rzp_test_THNrx21lyjVEE6",
    amount: totalAmount,
    currency: "INR",
    name: "DineSpot Order",
    description: "Table Reservation & Food Payment",
    image: "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=100&q=80",
    handler: function (response) {
      document.getElementById('successPaymentId').innerText = response.razorpay_payment_id;
      document.getElementById('successOrderId').innerText = "DS_" + Math.floor(Math.random() * 899999 + 100000);
      cart = [];
      updateCartUI();
      navigateTo('viewPaymentSuccess');
    },
    prefill: {
      name: currentUser ? currentUser.email.split('@')[0] : "Guest Diner",
      email: currentUser ? currentUser.email : "guest@dinespot.com",
      contact: "9876543210"
    },
    theme: {
      color: "#118D9D"
    }
  };

  const rzp = new Razorpay(options);
  rzp.open();
}

function renderBookingHistory() {
  const container = document.getElementById('bookingHistoryList');
  if (!container) return;

  if (bookings.length === 0) {
    container.innerHTML = `<p style="text-align: center; color: var(--text-muted); padding: 2rem;">No table reservations or past orders yet.</p>`;
    return;
  }

  container.innerHTML = bookings.map(b => `
    <div class="card" style="padding: 1.2rem; margin-bottom: 1rem;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
        <div style="font-weight: 700; font-size: 1.1rem; color: var(--primary);">${b.restaurantName || 'DineSpot Restaurant'}</div>
        <span class="chip active" style="font-size: 0.75rem; padding: 0.2rem 0.6rem;">${b.status || 'Pending'}</span>
      </div>
      <div style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 0.5rem;">
        <i class="far fa-calendar-alt"></i> ${b.dateTime || 'Today'} &nbsp;|&nbsp; <i class="far fa-clock"></i> ${b.timeSlot || '07:30 PM'} &nbsp;|&nbsp; <i class="fas fa-chair"></i> ${b.tableType || '4 Seater'}
      </div>
      ${b.specialRequests ? `<div style="font-size: 0.8rem; background: var(--bg); padding: 0.4rem 0.8rem; border-radius: 8px; margin-bottom: 0.5rem;">Requests: "${b.specialRequests}"</div>` : ''}
    </div>
  `).join('');
}

function filterCategory(cat, element) {
  document.querySelectorAll('#userFilterBar .chip').forEach(c => c.classList.remove('active'));
  element.classList.add('active');

  if (cat === 'All') {
    renderRestaurants(restaurants);
  } else {
    const filtered = restaurants.filter(r => r.category === cat);
    renderRestaurants(filtered);
  }
}

function handleSearch(e) {
  const query = e.target.value.toLowerCase();
  const filtered = restaurants.filter(r =>
    r.name.toLowerCase().includes(query) ||
    (r.category && r.category.toLowerCase().includes(query)) ||
    r.address.toLowerCase().includes(query)
  );
  renderRestaurants(filtered);
}

function renderAdminDashboard() {
  document.getElementById('adminTotalCount').innerText = restaurants.length;
  document.getElementById('adminOpenCount').innerText = restaurants.length;
  document.getElementById('adminFullCount').innerText = "0";
  document.getElementById('adminClosedCount').innerText = "0";

  renderFloorPlanGrid();
  renderAdminBookingsList();
  renderAnalyticsChart();
}

function renderFloorPlanGrid() {
  const floorGrid = document.getElementById('adminFloorGrid');
  if (!floorGrid) return;

  const tables = [
    { num: 1, type: '2 Seater', status: 'available' },
    { num: 2, type: '4 Seater', status: 'reserved' },
    { num: 3, type: '4 Seater', status: 'occupied' },
    { num: 4, type: '6 Seater', status: 'available' },
    { num: 5, type: '2 Seater', status: 'reserved' },
    { num: 6, type: '10 Seater', status: 'available' }
  ];

  floorGrid.innerHTML = tables.map(t => `
    <div class="table-node ${t.status}" onclick="toggleTableStatus(this)">
      <div style="font-weight: 800; font-size: 1.1rem;">Table ${t.num}</div>
      <div style="font-size: 0.75rem; margin: 0.2rem 0;">${t.type}</div>
      <span style="font-size: 0.7rem; font-weight: 700; text-transform: uppercase;">${t.status}</span>
    </div>
  `).join('');
}

function toggleTableStatus(el) {
  if (el.classList.contains('available')) {
    el.className = 'table-node reserved';
    el.querySelector('span').innerText = 'reserved';
  } else if (el.classList.contains('reserved')) {
    el.className = 'table-node occupied';
    el.querySelector('span').innerText = 'occupied';
  } else {
    el.className = 'table-node available';
    el.querySelector('span').innerText = 'available';
  }
}

function renderAdminBookingsList() {
  const container = document.getElementById('adminBookingsTable');
  if (!container) return;

  if (bookings.length === 0) {
    container.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 1rem;">No active reservation requests.</td></tr>`;
    return;
  }

  container.innerHTML = bookings.map(b => `
    <tr style="border-bottom: 1px solid var(--divider);">
      <td style="padding: 0.8rem; font-weight: 600;">${b.userName || 'Diner'}</td>
      <td style="padding: 0.8rem;">${b.restaurantName || 'DineSpot'}</td>
      <td style="padding: 0.8rem;">${b.dateTime || 'Today'} (${b.timeSlot || '07:30 PM'})</td>
      <td style="padding: 0.8rem;">${b.tableType || '4-Seater'}</td>
      <td style="padding: 0.8rem;"><span style="color: var(--success); font-weight: 700;">${b.status || 'Pending'}</span></td>
      <td style="padding: 0.8rem;">
        <button class="btn btn-outline" style="padding: 0.2rem 0.5rem; font-size: 0.75rem;" onclick="alert('Booking Approved')">Approve</button>
      </td>
    </tr>
  `).join('');
}

function renderAnalyticsChart() {
  const ctx = document.getElementById('analyticsChart')?.getContext('2d');
  if (!ctx) return;

  if (window.myDineChart) {
    window.myDineChart.destroy();
  }

  window.myDineChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
      datasets: [{
        label: 'Weekly Revenue (₹)',
        data: [15000, 22000, 18000, 29000, 35000, 52000, 58000],
        borderColor: '#118D9D',
        backgroundColor: 'rgba(17, 141, 157, 0.1)',
        fill: true,
        tension: 0.4
      }]
    },
    options: {
      responsive: true,
      plugins: {
        legend: { display: true }
      }
    }
  });
}

function switchNavTab(tabName) {
  document.querySelectorAll('.bottom-nav .nav-tab').forEach(t => t.classList.remove('active'));
  event.currentTarget.classList.add('active');

  if (tabName === 'home') navigateTo('viewHome');
  else if (tabName === 'cart') navigateTo('viewCart');
  else if (tabName === 'history') navigateTo('viewBookingHistory');
  else if (tabName === 'chat') navigateTo('viewUserChat');
  else if (tabName === 'profile') navigateTo('viewUserProfile');
}

function openChatModal() {
  navigateTo('viewUserChat');
  listenToLiveChat();
}

function listenToLiveChat() {
  const userId = currentUser ? currentUser.uid : "guest_user";
  const chatRef = rtdb.ref(`chats/${userId}`);

  chatRef.on('value', snapshot => {
    const data = snapshot.val();
    const chatContainer = document.getElementById('chatMessages');
    if (!chatContainer) return;

    if (!data) {
      chatContainer.innerHTML = `<p style="text-align: center; color: var(--text-muted); padding: 1rem;">Send a message to start support chat...</p>`;
      return;
    }

    const messages = Object.values(data);
    chatContainer.innerHTML = messages.map(m => `
      <div style="align-self: ${m.senderId === userId ? 'flex-end' : 'flex-start'}; background: ${m.senderId === userId ? 'var(--primary)' : 'var(--divider)'}; color: ${m.senderId === userId ? 'white' : 'var(--text)'}; padding: 0.6rem 1rem; border-radius: 12px; max-width: 80%; font-size: 0.85rem;">
        ${m.message}
      </div>
    `).join('');
    chatContainer.scrollTop = chatContainer.scrollHeight;
  });
}

function sendChatMessage() {
  const input = document.getElementById('chatInput');
  const msg = input.value.trim();
  if (!msg) return;

  const userId = currentUser ? currentUser.uid : "guest_user";
  const chatRef = rtdb.ref(`chats/${userId}`);

  chatRef.push({
    message: msg,
    senderId: userId,
    timestamp: Date.now()
  });

  input.value = '';
}

function toggleTheme() {
  const currentTheme = document.body.getAttribute('data-theme');
  const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
  document.body.setAttribute('data-theme', newTheme);
}

function logout() {
  auth.signOut().then(() => navigateTo('viewWelcome'));
}
