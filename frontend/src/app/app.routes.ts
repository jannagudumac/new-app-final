import { Routes } from '@angular/router';

import { DashboardComponent } from './components/dashboard/dashboard.component';
import { LayoutComponent } from './components/layout/layout.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { WallsComponent } from './components/walls/walls.component';
import { WallDetailComponent } from './components/wall-detail/wall-detail.component';
import { CatalogComponent } from './components/catalog/catalog.component';
import { CatalogDetailComponent } from './components/catalog-detail/catalog-detail.component';
import { ProfileComponent } from './components/profile/profile.component';
import { ConcertsComponent } from './components/concerts/concerts.component';
import { FriendsComponent } from './components/friends/friends.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'walls', component: WallsComponent },
      { path: 'walls/:id', component: WallDetailComponent },
      { path: 'catalog', component: CatalogComponent },
      { path: 'catalog/:type/:id', component: CatalogDetailComponent },
      { path: 'profile', component: ProfileComponent },
      { path: 'users/:username', component: ProfileComponent },
      { path: 'friends', component: FriendsComponent },
      { path: 'concerts', component: ConcertsComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
