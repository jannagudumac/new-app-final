import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, Subject, takeUntil } from 'rxjs';

import {
  CreateMusicItemRequest,
  ListeningStatus,
  MusicItem,
  MusicSection,
  MusicWallDetail,
  SectionNoteColor,
  WallWallpaper
} from '../../models/music-wall.model';
import { MusicWallService } from '../../services/music-wall.service';
import { CatalogService } from '../../services/catalog.service';
import { CommunityService } from '../../services/community.service';
import { Album, Track } from '../../models/catalog.model';
import { Friend, WallMember } from '../../models/community.model';
import { AuthService } from '../../services/auth.service';
import { PageHeaderService } from '../../services/page-header.service';

@Component({
  selector: 'app-wall-detail',
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
  templateUrl: './wall-detail.component.html',
  styleUrl: './wall-detail.component.css'
})
export class WallDetailComponent implements OnInit, OnDestroy {

  sectionColors: { value: SectionNoteColor; label: string; hex: string }[] = [
    { value: 'CREAM', label: 'Cream', hex: '#fffbea' },
    { value: 'ROSE', label: 'Rose', hex: '#fff0f3' },
    { value: 'PEACH', label: 'Peach', hex: '#fff2df' },
    { value: 'MINT', label: 'Mint', hex: '#eef9ec' },
    { value: 'SKY', label: 'Sky', hex: '#eef7ff' },
    { value: 'LAVENDER', label: 'Lavender', hex: '#f4efff' }
  ];

  wallpaperOptions: { value: WallWallpaper; label: string }[] = [
    { value: 'NONE', label: 'No wallpaper' },
    { value: 'IMAGE_1', label: 'Image 1' },
    { value: 'IMAGE_2', label: 'Image 2' },
    { value: 'IMAGE_3', label: 'Image 3' },
    { value: 'IMAGE_4', label: 'Image 4' },
    { value: 'IMAGE_5', label: 'Image 5' },
    { value: 'IMAGE_6', label: 'Image 6' },
    { value: 'IMAGE_7', label: 'Image 7' },
    { value: 'IMAGE_8', label: 'Image 8' },
    { value: 'IMAGE_9', label: 'Image 9' }
  ];

  wall: MusicWallDetail | null = null;
  wallId: number;
  loading = false;
  saving = false;
  appearanceOpen = false;
  errorMessage = '';
  catalogTracks: Track[] = [];
  catalogAlbums: Album[] = [];
  catalogQuery = '';
  catalogSearching = false;
  catalogSearchMessage = '';
  members: WallMember[] = [];
  friends: Friend[] = [];
  invitedFriendNames = new Set<string>();
  inviteUsername = '';
  inviteMessage = '';
  inviteError = '';
  inviting = false;
  showInviteForm = false;
  showSectionForm = false;
  editingWallField: 'name' | 'description' | null = null;
  editingWallValue = '';

  sectionForm: FormGroup;

  editingSectionId: number | null = null;
  editingSectionName = '';
  colorPickerSectionId: number | null = null;
  activeItemFormSectionId: number | null = null;
  private catalogSearchChanges = new Subject<string>();
  private destroy$ = new Subject<void>();
  private lastCatalogSearch = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private formBuilder: FormBuilder,
    private musicWallService: MusicWallService,
    private catalogService: CatalogService,
    private communityService: CommunityService,
    private pageHeaderService: PageHeaderService,
    public authService: AuthService
  ) {
    this.wallId = Number(this.route.snapshot.paramMap.get('id'));

    this.sectionForm = this.formBuilder.group({
      name: ['', [Validators.required, Validators.maxLength(80)]],
      noteColor: ['CREAM']
    });

  }

  ngOnInit(): void {
    if (Number.isNaN(this.wallId)) {
      this.router.navigate(['/walls']);
      return;
    }

    this.loadWall();
    this.loadMembers();
    this.loadFriends();
    this.catalogSearchChanges.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(query => this.searchCatalogue(query));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.pageHeaderService.clear();
  }

  toggleAppearance(): void {
    this.appearanceOpen = !this.appearanceOpen;
  }

  startWallFieldEdit(field: 'name' | 'description'): void {
    if (!this.wall || this.wall.ownerUsername !== this.authService.getUsername()) return;
    this.editingWallField = field;
    this.editingWallValue = field === 'name' ? this.wall.name : (this.wall.description || '');
    setTimeout(() => document.getElementById('wall-' + field + '-input')?.focus());
  }

  cancelWallFieldEdit(): void {
    this.editingWallField = null;
    this.editingWallValue = '';
  }

  saveWallField(): void {
    if (!this.wall || !this.editingWallField) return;
    const value = this.editingWallValue.trim();
    if (this.editingWallField === 'name' && !value) return;

    this.musicWallService.updateWall(this.wallId, {
      name: this.editingWallField === 'name' ? value : this.wall.name,
      description: this.editingWallField === 'description' ? value : (this.wall.description || ''),
      wallpaper: this.wall.wallpaper,
      wallColor: this.wall.wallColor
    }).subscribe({
      next: updated => {
        if (!this.wall) return;
        this.wall.name = updated.name;
        this.wall.description = updated.description;
        this.pageHeaderService.show(updated.name);
        this.cancelWallFieldEdit();
      },
      error: error => this.handleSaveError(error, 'Could not update wall')
    });
  }

  loadWall(): void {
    this.loading = true;
    this.errorMessage = '';

    this.musicWallService.getWall(this.wallId).subscribe({
      next: wall => {
        this.wall = wall;
        this.pageHeaderService.show(wall.name);
        this.loading = false;
        this.scrollToReturnSection();
      },
      error: error => {
        this.loading = false;
        this.errorMessage = error.error?.message || 'Could not load wall';
      }
    });
  }

  createSection(): void {
    if (this.sectionForm.invalid) {
      this.sectionForm.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.musicWallService.createSection(this.wallId, this.sectionForm.value).subscribe({
      next: () => {
        this.sectionForm.reset({ name: '', noteColor: 'CREAM' });
        this.showSectionForm = false;
        this.saving = false;
        this.loadWall();
      },
      error: error => this.handleSaveError(error, 'Could not create section')
    });
  }

  openSectionForm(): void {
    this.showSectionForm = true;
  }

  closeSectionForm(): void {
    this.showSectionForm = false;
    this.sectionForm.reset({ name: '', noteColor: 'CREAM' });
  }

  startSectionEdit(sectionId: number, name: string): void {
    this.colorPickerSectionId = null;
    this.editingSectionId = sectionId;
    this.editingSectionName = name;
    setTimeout(() => document.getElementById('section-name-' + sectionId)?.focus());
  }

  cancelSectionEdit(): void {
    this.editingSectionId = null;
    this.editingSectionName = '';
  }

  updateSection(sectionId: number, noteColor: SectionNoteColor): void {
    if (!this.editingSectionName.trim()) {
      return;
    }

    this.musicWallService.updateSection(
      this.wallId,
      sectionId,
      { name: this.editingSectionName.trim(), noteColor: noteColor }
    ).subscribe({
      next: () => {
        this.cancelSectionEdit();
        this.loadWall();
      },
      error: error => this.handleSaveError(error, 'Could not update section')
    });
  }

  toggleSectionColorPicker(sectionId: number): void {
    this.colorPickerSectionId = this.colorPickerSectionId === sectionId ? null : sectionId;
  }

  changeSectionColor(section: MusicSection, noteColor: SectionNoteColor): void {
    const previousColor = section.noteColor;
    section.noteColor = noteColor;
    this.colorPickerSectionId = null;

    this.musicWallService.updateSection(
      this.wallId,
      section.id,
      { name: section.name, noteColor: noteColor }
    ).subscribe({
      error: error => {
        section.noteColor = previousColor;
        this.handleSaveError(error, 'Could not change section color');
      }
    });
  }

  deleteSection(sectionId: number, sectionName: string): void {
    if (!window.confirm('Delete the section "' + sectionName + '" and all its items?')) {
      return;
    }

    this.musicWallService.deleteSection(this.wallId, sectionId).subscribe({
      next: () => this.loadWall(),
      error: error => this.handleSaveError(error, 'Could not delete section')
    });
  }

  openNewItemForm(sectionId: number): void {
    this.activeItemFormSectionId = sectionId;
    this.catalogQuery = '';
    this.catalogTracks = [];
    this.catalogAlbums = [];
    this.catalogSearchMessage = '';
    this.lastCatalogSearch = '';
    setTimeout(() => document.getElementById('catalog-search-' + sectionId)?.focus());
  }

  closeItemForm(): void {
    this.activeItemFormSectionId = null;
    this.catalogQuery = '';
    this.catalogTracks = [];
    this.catalogAlbums = [];
    this.catalogSearchMessage = '';
  }

  addCatalogResult(section: MusicSection, itemType: 'TRACK' | 'ALBUM', item: Track | Album): void {
    if (this.saving || !item.id || this.isCatalogResultAdded(section, itemType, item.id)) return;
    const request: CreateMusicItemRequest = {
      title: item.title,
      artist: item.artistName,
      itemType,
      status: 'TO_LISTEN',
      catalogTrackId: itemType === 'TRACK' ? item.id : null,
      catalogAlbumId: itemType === 'ALBUM' ? item.id : null
    };
    this.saving = true;

    this.musicWallService.createItem(this.wallId, section.id, request).subscribe({
      next: created => {
        section.items.push(created);
        this.saving = false;
      },
      error: error => this.handleSaveError(error, 'Could not create music item')
    });
  }

  isCatalogResultAdded(section: MusicSection, itemType: 'TRACK' | 'ALBUM', id: number): boolean {
    return section.items.some(item => itemType === 'TRACK'
      ? item.catalogTrackId === id
      : item.catalogAlbumId === id);
  }

  toggleStatus(sectionId: number, item: MusicItem): void {
    const newStatus: ListeningStatus =
      item.status === 'LISTENED' ? 'TO_LISTEN' : 'LISTENED';

    const request: CreateMusicItemRequest = {
      title: item.title,
      artist: item.artist,
      itemType: item.itemType,
      status: newStatus,
      catalogTrackId: item.catalogTrackId,
      catalogAlbumId: item.catalogAlbumId
    };

    this.musicWallService.updateItem(
      this.wallId,
      sectionId,
      item.id,
      request
    ).subscribe({
      next: updatedItem => item.status = updatedItem.status,
      error: error => this.handleSaveError(error, 'Could not update listening status')
    });
  }

  changeWallpaper(wallpaper: WallWallpaper): void {
    if (!this.wall) {
      return;
    }

    this.wall.wallpaper = wallpaper;
    this.musicWallService.updateWallAppearance(this.wallId, {
      wallpaper: wallpaper,
      wallColor: this.wall.wallColor
    }).subscribe({
      error: error => this.handleSaveError(error, 'Could not change wallpaper')
    });
  }

  changeWallColor(color: string): void {
    if (!this.wall) {
      return;
    }

    this.wall.wallColor = color;
    this.musicWallService.updateWallAppearance(this.wallId, {
      wallpaper: this.wall.wallpaper,
      wallColor: color
    }).subscribe({
      error: error => this.handleSaveError(error, 'Could not change wall color')
    });
  }

  deleteItem(sectionId: number, item: MusicItem): void {
    if (!window.confirm('Delete "' + item.title + '"?')) {
      return;
    }

    this.musicWallService.deleteItem(this.wallId, sectionId, item.id).subscribe({
      next: () => this.loadWall(),
      error: error => this.handleSaveError(error, 'Could not delete music item')
    });
  }

  queueCatalogueSearch(query: string): void {
    const cleaned = query.trim();
    if (cleaned.length < 2) {
      this.catalogTracks = [];
      this.catalogAlbums = [];
      this.catalogSearchMessage = cleaned ? 'Enter at least two characters.' : '';
      this.lastCatalogSearch = '';
    }
    this.catalogSearchChanges.next(cleaned);
  }

  searchCatalogue(requestedQuery?: string): void {
    const query = (requestedQuery ?? this.catalogQuery).trim();
    if (query.length < 2) {
      return;
    }
    if (query === this.lastCatalogSearch && (this.catalogSearching || this.catalogTracks.length || this.catalogAlbums.length)) return;

    this.lastCatalogSearch = query;
    this.catalogSearching = true;
    this.catalogSearchMessage = '';
    this.catalogService.search(query).subscribe({
      next: result => {
        this.catalogTracks = result.tracks;
        this.catalogAlbums = result.albums;
        this.catalogSearching = false;
        if (!result.tracks.length && !result.albums.length) {
          this.catalogSearchMessage = 'No tracks or albums found in the catalogue.';
        }
      },
      error: error => {
        this.catalogSearching = false;
        this.catalogSearchMessage = error.error?.message || 'Could not search the catalogue';
      }
    });
  }

  inviteMember(): void {
    const username = this.inviteUsername.trim();
    if (!username || this.inviting) return;

    this.inviting = true;
    this.inviteMessage = '';
    this.inviteError = '';
    this.communityService.invite(this.wallId, username).subscribe({
      next: () => {
        this.inviteMessage = 'Invitation sent to ' + username + '.';
        this.invitedFriendNames.add(username);
        this.inviteUsername = '';
        this.inviting = false;
      },
      error: error => {
        this.inviteError = error.error?.message || 'Could not send invitation';
        this.inviting = false;
      }
    });
  }

  toggleInviteForm(): void {
    this.showInviteForm = !this.showInviteForm;
    this.inviteMessage = '';
    this.inviteError = '';
  }

  private loadMembers(): void { this.communityService.getMembers(this.wallId).subscribe({ next: value => this.members = value }); }

  get availableFriends(): Friend[] {
    const memberNames = new Set(this.members.map(member => member.username));
    return this.friends.filter(friend =>
      !memberNames.has(friend.username) && !this.invitedFriendNames.has(friend.username)
    );
  }

  private loadFriends(): void {
    this.communityService.getFriends().subscribe({
      next: friends => this.friends = friends,
      error: () => this.friends = []
    });
  }

  private scrollToReturnSection(): void {
    const sectionId = this.route.snapshot.fragment;
    if (!sectionId || !/^section-\d+$/.test(sectionId)) return;

    setTimeout(() => {
      document.getElementById(sectionId)?.scrollIntoView({ block: 'center' });
    });
  }

  private handleSaveError(error: any, fallbackMessage: string): void {
    this.saving = false;
    this.errorMessage = error.error?.message || fallbackMessage;
  }
}
