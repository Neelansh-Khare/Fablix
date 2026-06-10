# FabFlix Local Test Checklist

## Prerequisites — One-Time Setup

### 1. Get a TMDB API Key (for movie posters)
1. Create a free account at https://www.themoviedb.org/signup
2. Go to **Settings → API** and request a Developer key
3. Copy the **API Key (v3 auth)** value

### 2. Create `.env` in the project root
Create a file named `.env` (next to `docker-compose.yml`) with the following content:

```
TMDB_API_KEY=your_tmdb_key_here
RECAPTCHA_SECRET=6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe
```

> The `RECAPTCHA_SECRET` above is Google's public test key — it always passes verification and is safe for local dev. Replace with a real key from https://www.google.com/recaptcha/admin before deploying.
>
> If you skip `TMDB_API_KEY`, the app still works but movie posters won't load from TMDB.

### 3. Verify Docker Desktop is running
Docker Desktop must be open and the Docker engine running before you proceed.

### 4. (Optional) Wipe previous volumes for a clean slate
If you've run the stack before and want a fresh database:
```
docker-compose down -v
```

---

## 0. Startup
- [ ] `docker-compose up --build -d` completes without errors
- [ ] `docker-compose ps` — all services `Up` (especially `fabflix-app1`, `fabflix-app2`, `fabflix-lb`, `fabflix-db-primary`)
- [ ] `http://localhost` loads the FabFlix homepage
- [ ] `http://localhost/fabflix/api/health` returns `{"status":"UP"}`

---

## 1. Auth — Registration & Login
- [ ] Click **Register** → fill in all fields → submit → success message shown
- [ ] Weak password (e.g. `abc`) → password validation error shown client-side before submit
- [ ] Login with wrong password → error message shown (not a stack trace)
- [ ] Login with correct credentials → welcome message, header updates to show your name
- [ ] **DevTools → Network**: after login, the response JSON contains `csrfToken`
- [ ] After login, any POST request (e.g. add to cart) has `X-CSRF-Token` header in Network tab
- [ ] Logout → header resets to guest state

---

## 2. Home Page — Movie Grid
- [ ] Movies load on homepage
- [ ] Movie poster images display (or placeholder if TMDB key not set)
- [ ] Clicking a movie card opens the movie detail page

---

## 3. Search & Autocomplete
- [ ] Type 3+ chars in search bar → dropdown suggestions appear
- [ ] Suggestions include both movies and stars
- [ ] Click a suggestion → navigates to that movie/star
- [ ] Press Enter or click Search button → search results page loads
- [ ] Search by title (e.g. "Batman") returns relevant movies
- [ ] Empty search → handled gracefully (no crash)

---

## 4. Browse
- [ ] **Browse** menu → shows genres and alphabet letters
- [ ] Click a genre → movie list filtered to that genre
- [ ] Click a letter → movies starting with that letter
- [ ] Pagination works (Next/Prev or page numbers)

---

## 5. Movie Detail Page
- [ ] All movie metadata shows: title, year, director, genres, stars, rating
- [ ] Stars are clickable → navigates to star detail page
- [ ] Star detail page shows the star's filmography
- [ ] **Add to Cart** button works → cart count in header increments
- [ ] Similar movies / recommendations section renders (if data exists)

---

## 6. Cart
- [ ] Cart page shows all added items with quantities and prices
- [ ] **+** / **−** quantity buttons update the quantity
- [ ] Decrease quantity to 0 → item removed from cart
- [ ] **Remove** button removes the item
- [ ] Total updates correctly after each change
- [ ] Cart persists if you navigate away and come back
- [ ] Cart reachable while logged out? → should prompt login

---

## 7. Checkout
- [ ] Click **Checkout** → payment form shown with order summary
- [ ] Submit with empty fields → browser/client validation fires
- [ ] Fill in valid CC info → order completes, success message shown
- [ ] After successful order, cart is empty
- [ ] **Profile → Order History** shows the new order

---

## 8. Profile
- [ ] Profile page loads: shows name, email, address
- [ ] Edit name/address → save → changes persist (refresh the page)
- [ ] Change password (requires current password) → works
- [ ] Wrong current password on change → error message shown
- [ ] Order history lists past orders with line items

---

## 9. CSRF Protection
- [ ] Open browser console and run:
  ```javascript
  $.ajax({ url: 'fabflix/api/cart', method: 'POST', data: { movieId: 'tt0000001', action: 'add' } })
  ```
  from a **different tab** where you're not logged in (or clear `window.csrfToken` first) → should get 403
- [ ] Normal cart add from the UI (where token is set) → works fine

---

## 10. Rate Limiting
- [ ] Log out, try logging in with wrong password **6 times** → account should lock with a message after 5 failures
- [ ] Wait is not required to confirm message — just verify the lockout message appears

---

## 11. Admin Dashboard
- [ ] Navigate to `http://localhost/_dashboard.jsp` (or however it's linked)
- [ ] Analytics section shows cache hit/miss counts
- [ ] **Add Movie** form → fill in title, year, director, star, genre → submit → success
- [ ] **Add Star** form → submit → success
- [ ] Poster update: check TMDB status shows configured/not configured

---

## 12. Error Handling
- [ ] Navigate to `http://localhost/fabflix/api/movies/INVALID!!!ID` → 400 or 404, no stack trace in response
- [ ] Navigate to a non-existent page → 404 error page loads
- [ ] All error responses are generic messages (no `e.getMessage()` leaks)

---

## Things to watch in DevTools Console the whole time
- No JavaScript errors (red entries)
- No `403 Forbidden` on normal user actions
- No responses containing Java exception messages or stack traces
